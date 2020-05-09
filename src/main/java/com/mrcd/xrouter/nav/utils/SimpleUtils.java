package com.mrcd.xrouter.nav.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

public class SimpleUtils {

    @Nullable
    public static PsiClass findClassInGlobalProject(String className, Project project) {
        return JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
    }

    /**
     * 从Router类中获取Activity的类名
     *
     * @param routerClass Router类
     * @param project     Project对象
     * @return 常量类名
     */
    @Nullable
    public static String findNameInRouterClass(PsiClass routerClass, Project project) {
        if (null == routerClass) {
            return "## router class is null";
        }
        //从类中搜索名为Name的静态常量，不需要查找父类
        PsiField nameField = routerClass.findFieldByName("NAME", false);
        if (null == nameField) {
            return "## NAME is not found";
        }
        PsiExpression initializer = nameField.getInitializer();
        if (initializer != null) {
            return initializer.getText().trim().replaceAll("\"", "");
        }
        return "## NAME field expression is null";
    }
}
