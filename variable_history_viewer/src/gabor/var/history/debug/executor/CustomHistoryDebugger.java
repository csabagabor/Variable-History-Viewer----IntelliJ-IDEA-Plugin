package gabor.var.history.debug.executor;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.ui.RunContentDescriptor;
import gabor.var.history.ResourcesPlugin;
import gabor.var.history.action.CoverageContext;
import gabor.var.history.helper.LoggingHelper;
import gabor.var.history.helper.PluginHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomHistoryDebugger extends GenericDebuggerRunner {

    @NotNull
    @Override
    public String getRunnerId() {
        return ResourcesPlugin.DEBUGGER_NAME;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(ResourcesPlugin.DEBUGGER_NAME) && !(profile instanceof RunConfigurationWithSuppressedDefaultRunAction) &&
                profile instanceof RunConfigurationBase;
    }

    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        try {
            JavaParameters javaParameters = ((JavaCommandLine) state).getJavaParameters();

            CoverageContext context = CoverageContext.getInstance(env.getProject());
            context.createOwnTempFile();//create files only when using the custom runner, do not slow down user's normal runner/debugger
            context.writePatternsToTempFile();//create files only when using the custom runner, do not slow down user's normal runner/debugger
            javaParameters.getVMParametersList().addAll(getStartupParameters(context));
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
        return super.doExecute(state, env);
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
