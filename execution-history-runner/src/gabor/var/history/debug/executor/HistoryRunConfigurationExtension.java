package gabor.var.history.debug.executor;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import gabor.var.history.action.CoverageContext;
import gabor.var.history.helper.LoggingHelper;
import gabor.var.history.helper.PluginHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HistoryRunConfigurationExtension extends RunConfigurationExtension {

    @Override
    public <T extends RunConfigurationBase> void updateJavaParameters(@NotNull T configuration, @NotNull JavaParameters javaParameters, RunnerSettings runnerSettings) {
        try {
            if (runnerSettings instanceof HistoryExecutorData) {
                try {
                    CoverageContext context = CoverageContext.getInstance(configuration.getProject());
                    context.createOwnTempFile();//create files only when using the custom runner, do not slow down user's normal runner/debugger
                    context.writePatternsToTempFile();//create files only when using the custom runner, do not slow down user's normal runner/debugger
                    javaParameters.getVMParametersList().addAll(getStartupParameters(context));
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }


    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configurationBase) {
        return configurationBase instanceof CommonJavaRunConfigurationParameters;
    }

    public List<String> getStartupParameters(CoverageContext coverageContext) {
        Optional<String> pluginPath = PluginHelper.getAgentPath();

        List<String> res = new ArrayList<>();
        if (pluginPath.isPresent()) {
            res.add("-javaagent:" + pluginPath.get() + "=" + coverageContext.getAgentArgString());

            //ClassData and ProjectData needs to be loaded in the client app's code, this prevents NoClassDefFoundError
            res.add("-Xbootclasspath/a:" + pluginPath.get());
            res.add("-noverify");//works for java 8+
        }
        return res;
    }
}
