package com.kotori316.fluidtank.gametest;

import com.google.common.base.CaseFormat;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.gametest.cat.CatGameTest;
import com.kotori316.fluidtank.gametest.reservoir.ReservoirTest;
import com.kotori316.fluidtank.gametest.tank.TankTest;
import com.kotori316.testutil.common.TestFunction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class GameTestFunctions {
    public static final String BATCH = "from_common";

    public static List<TestFunction> createTestFunctionsNoPlace(String batchName, String structureName) {
        ResourceLocation.parse(structureName);
        List<Class<?>> classes = List.of(
        );
        var fromClass = getTestFunctionStream(batchName, structureName, classes, 3);
        return Stream.of(
            fromClass
        ).flatMap(Function.identity()).toList();
    }

    public static List<TestFunction> createTestFunctionsPlace(String batchName, String structureName) {
        ResourceLocation.parse(structureName);
        List<Class<?>> classes = List.of(
        );
        var fromClass = getTestFunctionStream(batchName, structureName, classes, 100);
        return Stream.of(
            fromClass,
            CatGameTest.tests(batchName, structureName),
            ReservoirTest.tests(batchName, structureName),
            TankTest.tests(batchName, structureName)
        ).flatMap(Function.identity()).toList();
    }

    public static @NotNull Stream<TestFunction> getTestFunctionStream(String batchName, String structureName, List<Class<?>> classes, int maxTicks) {
        return classes.stream()
            .flatMap(c -> Stream.of(c.getDeclaredMethods()))
            .filter(Predicate.not(Method::isSynthetic))
            .filter(m -> (m.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
            .filter(m -> (m.getModifiers() & Modifier.PRIVATE) != Modifier.PRIVATE)
            .filter(m -> m.getParameterCount() == 1)
            .filter(m -> m.getParameterTypes()[0] == GameTestHelper.class)
            .filter(m -> m.getReturnType() == void.class)
            .map(m ->
                TestFunction.createWithStructure(FluidTankCommon.modId, batchName, CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, "%s_%s".formatted(m.getDeclaringClass().getSimpleName(), m.getName())), structureName, g -> {
                    try {
                        m.setAccessible(true);
                        m.invoke(null, g);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof RuntimeException r) throw r;
                        else throw new RuntimeException(e.getCause());
                    } catch (ReflectiveOperationException | AssertionError e) {
                        throw new RuntimeException(e);
                    }
                })
            );
    }

    public static Consumer<GameTestHelper> wrapper(Consumer<GameTestHelper> test) {
        return g -> {
            try {
                test.accept(g);
            } catch (AssertionError e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static @NotNull TestFunction create(String batchName, String structureName, String name, Consumer<GameTestHelper> test) {
        var formatted = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
        String namespacedStructureName;
        if (structureName.equals(TestFunction.EMPTY_STRUCTURE)) {
            namespacedStructureName = "minecraft:empty";
        } else {
            namespacedStructureName = "%s:%s".formatted(FluidTankCommon.modId, structureName);
        }
        String namespacedBatchName = "%s:%s".formatted(FluidTankCommon.modId, ResourceLocation.parse(batchName).getPath());
        return TestFunction.createWithStructure(FluidTankCommon.modId, namespacedBatchName, formatted, namespacedStructureName, test);
    }

    public static void assertEqualStack(ItemStack expected, ItemStack actual) {
        Assertions.assertTrue(ItemStack.matches(expected, actual),
            "Expected: %s(%s), Actual: %s(%s)".formatted(expected, expected.getComponentsPatch(), actual, actual.getComponentsPatch()));
    }
}
