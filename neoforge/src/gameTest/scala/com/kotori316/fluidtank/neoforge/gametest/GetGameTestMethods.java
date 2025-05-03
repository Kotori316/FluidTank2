package com.kotori316.fluidtank.neoforge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.testutil.common.TestFunction;
import com.kotori316.testutil.common.TestFunctionRegister;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Mod("fluidtank_game_test")
public class GetGameTestMethods {
    public static final String DEFAULT_BATCH = "test";

    public GetGameTestMethods(IEventBus modBus) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Loaded FluidTank GameTest mod");
        modBus.addListener(GetGameTestMethods::registerGameTest);
    }

    public static void registerGameTest(FMLConstructModEvent event) {
        var tests = Stream.of(
            new CatTest().tests(),
            new FromCommon().createTestFunctionsPlace(),
            new FromCommon().createTestFunctionsNoPlace(),
            new FromCommon().load2032Tank(),
            new PlatformAccessTest().tests(),
            new RecipeTest().tests(),
            new SideProxyTest().generator(),
            new TankFluidHandlerTest().generator(),
            new TankPlacementTest().tests(),
            new TankTest().fillTest()
        ).flatMap(Collection::stream);
        tests.forEach(TestFunctionRegister::registerTestFunction);
    }

    static <T> List<TestFunction> getTests(Class<? extends T> clazz, T instance, String batchName) {
        var noArgs = getNoArgMethods(clazz)
            .map(m -> GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE,
                clazz.getSimpleName() + "_" + m.getName(),
                () -> ReflectionSupport.invokeMethod(m, instance)));
        var withHelper = getHelperArgMethods(clazz)
            .map(m -> GameTestFunctions.create(batchName, TestFunction.EMPTY_STRUCTURE,
                clazz.getSimpleName() + "_" + m.getName(),
                g -> ReflectionSupport.invokeMethod(m, instance, g)));
        return Stream.concat(noArgs, withHelper).toList();
    }

    static <T> List<TestFunction> getTests(Class<? extends T> clazz, T instance, String batchName, String structure) {
        var noArgs = getNoArgMethods(clazz)
            .map(m -> GameTestFunctions.create(batchName, structure,
                clazz.getSimpleName() + "_" + m.getName(),
                () -> ReflectionSupport.invokeMethod(m, instance)));
        var withHelper = getHelperArgMethods(clazz)
            .map(m -> GameTestFunctions.create(batchName, structure,
                clazz.getSimpleName() + "_" + m.getName(),
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
