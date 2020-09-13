package gabor.var.history.debug.view;

import org.jetbrains.annotations.NotNull;

public interface DebugStackFrameListener {
    void onChanged(@NotNull StackFrameManager stackFrameManager);
}
