package com.kotori316.fluidtank.neoforge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.testutil.GameTestUtil;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Mod("fluidtank_game_test")
public class GetGameTestMethods {
    public static final String DEFAULT_BATCH = "fluidtank.defaultBatch";

    public GetGameTestMethods() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Loaded FluidTank GameTest mod");
    }

    static <T> List<TestFunction> getTests(Class<? extends T> clazz, T instance, String batchName) {
        var noArgs = getNoArgMethods(clazz)
            .map(m -> GameTestUtil.create(FluidTankCommon.modId, batchName,
                clazz.getSimpleName() + "_" + m.getName(),
                () -> ReflectionSupport.invokeMethod(m, instance)));
        var withHelper = getHelperArgMethods(clazz)
            .map(m -> GameTestUtil.create(FluidTankCommon.modId, batchName,
                clazz.getSimpleName() + "_" + m.getName(),
                g -> ReflectionSupport.invokeMethod(m, instance, g)));
        return Stream.concat(noArgs, withHelper).toList();
    }

    static <T> List<TestFunction> getTests(Class<? extends T> clazz, T instance, String batchName, String structure) {
        var noArgs = getNoArgMethods(clazz)
            .map(m -> GameTestUtil.createWithStructure(FluidTankCommon.modId, batchName,
                clazz.getSimpleName() + "_" + m.getName(), structure,
                () -> ReflectionSupport.invokeMethod(m, instance)));
        var withHelper = getHelperArgMethods(clazz)
            .map(m -> GameTestUtil.createWithStructure(FluidTankCommon.modId, batchName,
                clazz.getSimpleName() + "_" + m.getName(), structure,
                g -> ReflectionSupport.invokeMethod(m, instance, g)));
        return Stream.concat(noArgs, withHelper).toList();
    }

    @NotNull
    private static <T> Stream<Method> getNoArgMethods(Class<? extends T> clazz) {
        return Stream.of(clazz.getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> (m.getModifiers() & (Modifier.PRIVATE | Modifier.STATIC)) == 0);
    }

    @NotNull
    private static <T> Stream<Method> getHelperArgMethods(Class<? extends T> clazz) {
        return Stream.of(clazz.getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{GameTestHelper.class}))
            .filter(m -> !m.isAnnotationPresent(GameTest.class))
            .filter(m -> (m.getModifiers() & (Modifier.PRIVATE | Modifier.STATIC)) == 0);
    }

    public static void assertEqualHelper(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, "Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static void assertEqualStack(ItemStack expected, ItemStack actual) {
        Assertions.assertTrue(ItemStack.matches(expected, actual),
            "Expected: %s(%s), Actual: %s(%s)".formatted(expected, expected.getComponentsPatch(), actual, actual.getComponentsPatch()));
    }
}
