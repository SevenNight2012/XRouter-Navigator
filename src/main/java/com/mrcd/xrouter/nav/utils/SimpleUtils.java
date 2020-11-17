package com.mrcd.xrouter.nav.utils;

import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.mrcd.xrouter.nav.notification.SimpleNotification;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

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

    /**
     * 展示弹窗提示
     *
     * @param event   鼠标行为
     * @param project Project对象
     */
    public static void showPopup(Project project, MouseEvent event, String title, String okText) {
        BaseListPopupStep<String> step = new BaseListPopupStep<String>(title, okText) {
            @Override
            public PopupStep<?> onChosen(String selectedValue, final boolean finalChoice) {
                return doFinalStep(() -> {
                });
            }

            @Override
            public boolean isMnemonicsNavigationEnabled() {
                return true;
            }
        };
        step.setDefaultOptionIndex(0);
        new ListPopupImpl(project, step).show(new RelativePoint(event));
    }

    /**
     * 首先调用低版本的api，进行搜索
     *
     * @param routerMethod 需要搜索的路由方法
     * @param event        event对象
     * @param elementRoot  element对象
     * @return api兼容是否有问题，如果在高版本的ide上应该调用{@link ShowUsagesAction#startFindUsages(PsiElement, RelativePoint, Editor)}
     */
    public static boolean findSearchInLowApi(PsiElement routerMethod, MouseEvent event, PsiElement elementRoot) {
        try {
            ShowUsagesAction action = new ShowUsagesAction();
            action.startFindUsages(routerMethod, new RelativePoint(event), PsiUtilBase.findEditor(elementRoot), 100);
            return false;
        } catch (Throwable throwable) {
            SimpleNotification notification = new SimpleNotification();
            notification.showError(throwable, elementRoot.getProject());
            return true;
        }
    }
}
