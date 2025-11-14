package com.shuyixiao.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Query;

public class CodeLocator {

    public static PsiClass findClass(Project project, String name) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass byFqn = facade.findClass(name, GlobalSearchScope.projectScope(project));
        if (byFqn != null) return byFqn;
        Query<PsiClass> q = AllClassesSearch.search(GlobalSearchScope.projectScope(project), project);
        for (PsiClass c : q) {
            if (c.getName() != null && c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static PsiFile findFile(Project project, String filename) {
        PsiFile[] files = com.intellij.psi.search.FilenameIndex.getFilesByName(project, filename, GlobalSearchScope.projectScope(project));
        return files.length > 0 ? files[0] : null;
    }

    public static PsiMethod findMethod(PsiClass cls, String methodName) {
        if (cls == null) return null;
        for (PsiMethod m : cls.getMethods()) {
            if (m.getName().equals(methodName)) return m;
        }
        return null;
    }

    public static String snippet(PsiElement element, int maxChars) {
        if (element == null) return "";
        String text = element.getText();
        if (text == null) return "";
        if (text.length() > maxChars) return text.substring(0, maxChars);
        return text;
    }

    public static int line(Project project, PsiElement element) {
        if (element == null) return 0;
        PsiFile file = element.getContainingFile();
        if (file == null) return 0;
        com.intellij.openapi.editor.Document doc = com.intellij.openapi.editor.EditorFactory.getInstance().createDocument(file.getText());
        int offset = element.getTextOffset();
        return doc.getLineNumber(offset) + 1;
    }

    public static VirtualFile vfile(PsiElement element) {
        return element == null ? null : PsiUtilCore.getVirtualFile(element);
    }

    public static void open(Project project, PsiElement element) {
        VirtualFile vf = vfile(element);
        if (vf == null) return;
        int line = line(project, element) - 1;
        new OpenFileDescriptor(project, vf, line, 0).navigate(true);
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, vf, line, 0), true);
    }
}

