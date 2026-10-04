/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Attaches {@link RetryAnalyzer} to Cucumber's single TestNG @Test method
 * ({@code AbstractTestNGCucumberTests#runScenario}). Registered per-runner via
 * {@code @Listeners(RetryAnnotationTransformer.class)} so it applies whether the runner is
 * launched directly (IntelliJ) or through {@code testng.xml}.
 */
public class RetryAnnotationTransformer implements IAnnotationTransformer {

	@SuppressWarnings("rawtypes")
	@Override
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor,
			Method testMethod) {
		annotation.setRetryAnalyzer(RetryAnalyzer.class);
	}
}
