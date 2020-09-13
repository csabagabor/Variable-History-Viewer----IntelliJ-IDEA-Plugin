package gabor.var.history.action;


import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.util.indexing.FileBasedIndex;
import gabor.var.history.helper.FileHelper;
import gabor.var.history.helper.LoggingHelper;
import gabor.var.history.helper.OnlyProjectSearchScope;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CoverageContext {
    private final Project myProject;
    private File tempFile;
    private Map<String, Integer> patterns;

    public CoverageContext(Project myProject) {
        this.myProject = myProject;
    }

    public static CoverageContext getInstance(Project project) {
        return ServiceManager.getService(project, CoverageContext.class);
    }

    public void createOwnTempFile() throws IOException {
        File temp;
        temp = FileHelper.createTempFile();
        temp.deleteOnExit();
        tempFile = new File(temp.getCanonicalPath());
    }

    public void writePatternsToTempFile() {
        try {
            patterns = new LinkedHashMap<>();//preserve order
            int index = 0;
            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance()
                    .getContainingFiles(
                            FileTypeIndex.NAME,
                            JavaFileType.INSTANCE,
                            new OnlyProjectSearchScope(myProject));

            for (VirtualFile virtualFile : containingFiles) {
                PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
                if (psiFile instanceof PsiJavaFile) {
                    PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                    PsiClass[] javaFileClasses = psiJavaFile.getClasses();

                    for (PsiClass javaFileClass : javaFileClasses) {
                        patterns.put(javaFileClass.getQualifiedName(), index);
                        index++;
                    }
                }
            }

            FileHelper.writeClasses(tempFile, patterns.keySet().toArray(new String[0]));
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    @NotNull
    public String getAgentArgString() {
        //there is a ~900 character length limit on the String
        return this.tempFile.getAbsolutePath();
    }

    public Map<String, Integer> getPatterns() {
        return patterns;
    }
}

