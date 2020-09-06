
package gabor.var.history.debug;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.vcs.log.Hash;
import com.sun.jdi.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import gabor.var.history.debug.type.PlainType;
import gabor.var.history.debug.type.StackFrame;
import gabor.var.history.debug.type.var.HistoryLocalVariable;
import gabor.var.history.debug.view.HistoryToolWindowService;
import gabor.var.history.helper.LoggingHelper;

import java.util.*;

public class DebugExtractor implements DebuggerCommand {
    private final DebugProcessImpl debugProcess;
    private ObjectReference objectReference;
    private String fieldName;
    private Map<String, Integer> patterns;
    private StackFrameProxyImpl frameProxy;
    private final SuspendContextImpl suspendContext;


    public DebugExtractor(DebugProcessImpl debugProcess, ObjectReference objectReference, String fieldName,
                          Map<String, Integer> patterns) {
        this.debugProcess = debugProcess;
        this.objectReference = objectReference;
        this.fieldName = fieldName;
        this.patterns = patterns;

        if (this.patterns == null) {
            this.patterns = new HashMap<>();
        }

        SuspendContextImpl suspendContext = debugProcess.getDebuggerContext().getSuspendContext();
        this.suspendContext = suspendContext;
    }

    @Override
    public void action() {
        frameProxy = suspendContext.getFrameProxy();
        ThreadReferenceProxyImpl myThreadProxy = frameProxy.threadProxy();
        ThreadReference threadRef = myThreadProxy.getThreadReference();

        //disable all types of requests, they can cause deadlock
        List<EventRequest> requests = new ArrayList<>();
        try {
            EventRequestManager manager = threadRef.virtualMachine().eventRequestManager();

            manager.breakpointRequests().forEach(er -> disableRequest(er, requests));
            manager.exceptionRequests().forEach(er -> disableRequest(er, requests));

            manager.classUnloadRequests().forEach(er -> disableRequest(er, requests));
            manager.classPrepareRequests().forEach(er -> disableRequest(er, requests));

            manager.methodEntryRequests().forEach(er -> disableRequest(er, requests));
            manager.methodExitRequests().forEach(er -> disableRequest(er, requests));

            manager.accessWatchpointRequests().forEach(er -> disableRequest(er, requests));
            manager.modificationWatchpointRequests().forEach(er -> disableRequest(er, requests));

            manager.threadStartRequests().forEach(er -> disableRequest(er, requests));
            manager.threadDeathRequests().forEach(er -> disableRequest(er, requests));

            manager.monitorContendedEnteredRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorContendedEnterRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorWaitedRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorWaitRequests().forEach(er -> disableRequest(er, requests));

            manager.vmDeathRequests().forEach(er -> disableRequest(er, requests));

            manager.stepRequests().forEach(er -> disableRequest(er, requests));
        } catch (Throwable e) {

        }

        try {
            try {
                ReferenceType referenceType = findReferenceType("var.modified.com.intellij.rt.coverage.data.Redirector");
                if (referenceType instanceof ClassType) {

                    ReferenceType systemRefType = findReferenceType("java.lang.System");
                    if (systemRefType instanceof ClassType) {
                        //Value hashcode = invokeMethod(((ClassType) systemRefType), "identityHashCode", Arrays.asList(objectReference));

                        StringReference stringReference = debugProcess.getVirtualMachineProxy().mirrorOf(fieldName);
                        Value value = invokeMethod(((ClassType) referenceType), "getAccess", Arrays.asList(objectReference, stringReference));

                        boolean isHistorySaved = false;
                        if (value instanceof ArrayReference) {
                            isHistorySaved = true;
                            extractAndShow((ArrayReference) value, false, 0);
                        } else {
                            for (int i = 0; i < 3; i++) {
                                IntegerValue indexOfList = debugProcess.getVirtualMachineProxy().mirrorOf(i);
                                value = invokeMethod(((ClassType) referenceType), "getAccessPrimitiveAt", Arrays.asList(objectReference, stringReference, indexOfList));

                                if (!(value instanceof ArrayReference)) {
                                    break;
                                }

                                isHistorySaved = true;
                                extractAndShow((ArrayReference) value, true, i);
                            }
                        }


                        if (!isHistorySaved) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Notification notification = NotificationGroup.toolWindowGroup("variable.history.notifications", ToolWindowId.DEBUG)
                                        .createNotification(
                                                "No History", "", "No history information for this variable",
                                                NotificationType.ERROR);
                                notification.notify(debugProcess.getProject());
                            });
                        }
                    }
                }
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }

            //resume requests
            for (EventRequest request : requests) {
                request.enable();
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    private void extractAndShow(ArrayReference value, boolean containsValue, int index) {
        List<Value> values = value.getValues();

        List<StackFrame> stackFrames = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Value stringValue = values.get(i);
            if (stringValue instanceof StringReference) {
                if (containsValue && i == values.size() - 1) {
                    String primitiveValue = ((StringReference) stringValue).value();
                    stackFrames.forEach(frame -> frame.setVars(Arrays.asList(new HistoryLocalVariable(fieldName, new PlainType(primitiveValue)))));
                    break;
                }

                extractStackFrame(stackFrames, (StringReference) stringValue);
            }
        }

        showExecutionPoint(stackFrames, index);
    }

    private void showExecutionPoint(List<StackFrame> stackFrames, int index) {
        if (stackFrames.size() > 0) {
            ApplicationManager.getApplication().invokeLater(() -> {
                HistoryToolWindowService.getInstance(debugProcess.getProject()).showToolWindow(
                        stackFrames, stackFrames.get(0), "field:" + fieldName + "#change nr=" + index);
            });
        }
    }

    private void extractStackFrame(List<StackFrame> stackFrames, StringReference stringValue) {
        String executionPoint = stringValue.value();
        String[] split = executionPoint.split("#");

        String fullClassName = split[0];
        String className = fullClassName;
        String methodName = split[1];
        int line = Integer.parseInt(split[2]) - 1;

        int lastIndexOfDot = fullClassName.lastIndexOf(".");
        if (lastIndexOfDot >= 0) {
            className = fullClassName.substring(lastIndexOfDot + 1);
        }

        stackFrames.add(new StackFrame(className, fullClassName, methodName, line, Arrays.asList(), patterns.containsKey(fullClassName)));
    }

    private ReferenceType findReferenceType(String name) {
        List<ReferenceType> referenceTypes = debugProcess.getVirtualMachineProxy().classesByName(name);
        if (referenceTypes != null && referenceTypes.size() > 0) {
            return referenceTypes.get(0);
        }

        return null;
    }

    private void disableRequest(EventRequest request, List<EventRequest> disabled) {
        request.disable();
        disabled.add(request);
    }

    private Value invokeMethod(ClassType classType, String methodName, List<Value> arguments) {
        try {
            List<Method> methods = classType.methodsByName(methodName);

            Method method = null;
            for (Method m : methods) {
                method = m;
                break;
            }

            if (method == null) {
                return null;
            }

            ThreadReference threadRef = frameProxy.threadProxy().getThreadReference();

            Value value = classType.invokeMethod(threadRef, method,
                    arguments, ObjectReference.INVOKE_SINGLE_THREADED);

            return value;
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
        return null;
    }

    @Override
    public void commandCancelled() {

    }
}