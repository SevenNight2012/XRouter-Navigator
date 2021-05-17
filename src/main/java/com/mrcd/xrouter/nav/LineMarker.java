package com.mrcd.xrouter.nav;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.search.JavaFilesSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Query;
import com.mrcd.xrouter.nav.notification.SimpleNotification;
import com.mrcd.xrouter.nav.utils.LogUtils;
import com.mrcd.xrouter.nav.utils.PsiUtils;
import com.mrcd.xrouter.nav.utils.SimpleUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.util.TextUtils;
import org.codehaus.groovy.runtime.memoize.LRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClassOrObject;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.intellij.codeInsight.hint.HintManager.*;
import static com.mrcd.xrouter.nav.utils.PsiUtils.XPATH_TAG;

/**
 * 入口类
 */
public class LineMarker implements LineMarkerProvider, GutterIconNavigationHandler<PsiElement> {

    private final Icon ICON_NAV = IconLoader.getIcon("/icons/nav.svg");
    private final int MY_HINT_FLAG = HIDE_BY_ANY_KEY | HIDE_BY_TEXT_CHANGE | HIDE_BY_SCROLLING;

    //缓存容量为16
    LRUCache mRouterCache = new LRUCache(1 << 4);

    @Override
    public @Nullable
    LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        Project project = element.getProject();

        if (PsiUtils.isXPathAnnotation(element)) {
            String text = element.getText();
            TextRange textRange = element.getTextRange();
            int pathIndex = text.indexOf(XPATH_TAG);
            //这是注解开始的地方，需要传入这个TextRange才能把图标正确定位到对应的行
            TextRange annotationTextRange = TextRange.create(textRange.getStartOffset() + pathIndex, textRange.getEndOffset());
            return new LineMarkerInfo<>(element, annotationTextRange, ICON_NAV, psiElement -> "Show invoker", this, Alignment.CENTER);
        }
        return createLauncherInvokeMarker(element, project);
    }

    /**
     * 启动路由的代码行标记，launch方法调用的地方
     *
     * @param element element对象
     * @param project Project对象
     * @return 创建的行标记对象
     */
    @Nullable
    private LineMarkerInfo<?> createLauncherInvokeMarker(@NotNull PsiElement element, Project project) {
        String routerClassName = PsiUtils.isRouterInvoke(element);
        if (TextUtils.isEmpty(routerClassName)) {
            return null;
        }

        PsiClass routerClass = SimpleUtils.findClassInGlobalProject(routerClassName, project);
        String nameValue = SimpleUtils.findNameInRouterClass(routerClass, project);

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(ICON_NAV);
        PsiElement targetElement = element;
        if (TextUtils.isEmpty(nameValue)) {
            builder.setTooltipText("NAME field is empty");
        } else if (nameValue.startsWith("## ")) {
            builder.setTooltipText(nameValue);
        } else {
            String tipText = "Goto target: ";
            targetElement = SimpleUtils.findClassInGlobalProject(nameValue, project);
            if (null == targetElement) {
                targetElement = element;
                tipText = "Class not found: " + nameValue;
            } else {
                tipText += ((PsiClass) targetElement).getQualifiedName();
            }
            builder.setTooltipText(tipText);
        }
        builder.setTargets(targetElement);
        return builder.createLineMarkerInfo(element);
    }

    @Override
    public void navigate(MouseEvent event, PsiElement element) {
        Project project = element.getProject();
        String elementName = getElementName(element);
        if (TextUtils.isEmpty(elementName)) {
            SimpleUtils.showPopup(project, event, "Error", "Element name is empty");
            return;
        }
        PsiMethod method = (PsiMethod) mRouterCache.get(elementName);
        if (method != null) {
            showUsage(event, element, project, method);
            return;
        }

        PsiClass coreAnnotation = SimpleUtils.findClassInGlobalProject(PsiUtils.NAVIGATION_FULL_NAME, project);
        if (coreAnnotation != null) {
            //查找Navigation的使用
            Query<PsiReference> psiReferences = ReferencesSearch.search(coreAnnotation, new JavaFilesSearchScope(project), false);
            //找出搜寻结果中所有的class对象
            List<PsiClass> classes = findClassWithAnnotation(psiReferences);
            if (CollectionUtils.isEmpty(classes)) {
                SimpleUtils.showPopup(project, event, "Error", "No class annotated @Navigation");
                return;
            }
            PsiMethod routerMethod = findRouterMethod(project, elementName, classes);
            if (null == routerMethod) {
                SimpleUtils.showPopup(project, event, "Error", "No method found with return type is " + elementName);
                return;
            }
            mRouterCache.put(elementName, routerMethod);
            showUsage(event, element, project, routerMethod);
        } else {
            SimpleUtils.showPopup(project, event, "Error", "Not found @Navigation,please update XRouter");
        }
    }

    private void showUsage(MouseEvent event, PsiElement element, Project project, PsiMethod routerMethod) {
        RelativePoint position = new RelativePoint(event);
        Editor editor = PsiUtilBase.findEditor(element);
        //notice 此处有版本兼容问题，此方法将从2020.3版本以后的IDE中移除
        if (SimpleUtils.findSearchInLowApi(routerMethod, event, element)) {
            try {
                ShowUsagesAction.startFindUsages(routerMethod, position, editor);
            } catch (Throwable throwable) {
                SimpleUtils.showPopup(project, event, "Error", "Api error!");
                SimpleNotification notification = new SimpleNotification();
                notification.showError(throwable, project);
            }
        }
    }

    /**
     * 从被 @Navigation 注解的类里面查找对应activity启动的方法
     * 即：XRouter.getInstance().mainActivity().set... 要找到mainActivity()此方法，获取它的返回值类型，然后匹配
     *
     * @param project     Project对象
     * @param elementName activity全路径
     * @param classes     被注解的XRouter类
     * @return 找到的方法
     */
    private PsiMethod findRouterMethod(Project project, String elementName, List<PsiClass> classes) {
        for (PsiClass clazz : classes) {
            PsiMethod[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                PsiMethod method = methods[i];
                PsiType returnType = method.getReturnType();
                if (null == returnType) {
                    continue;
                }
                String returnClassName = returnType.getCanonicalText();
                PsiClass returnClass = SimpleUtils.findClassInGlobalProject(returnClassName, project);
                if (null == returnClass) {
                    continue;
                }
                String routerClassName = SimpleUtils.findNameInRouterClass(returnClass, project);
                LogUtils.d(routerClassName);
                if (elementName.equals(routerClassName)) {
                    //发现两个类路径关联的相同，那么说明就是对应的启动方法，查找这个方法的调用，就能知道有哪些地方启动了这个Activity
                    return method;
                }
            }
        }
        return null;
    }

    private String getElementName(PsiElement element) {
        if (element instanceof PsiClass) {
            return ((PsiClass) element).getQualifiedName();
        } else if (element instanceof KtClassOrObject) {
            FqName name = ((KtClassOrObject) element).getFqName();
            return null != name ? name.asString() : "";
        }
        return "";
    }

    /**
     * 查找被Navigation注解的class，注意此处未兼容kotlin
     *
     * @param psiReferences psiReference对象
     * @return 被注解的class集合
     */
    private List<PsiClass> findClassWithAnnotation(Query<PsiReference> psiReferences) {
        List<PsiClass> classes = new ArrayList<>();
        Iterator<PsiReference> iterator = psiReferences.iterator();
        while (iterator.hasNext()) {
            PsiReference reference = iterator.next();
            PsiClass psiClass = getMasterClass(reference.getElement());
            if (null != psiClass) {
                LogUtils.d("Class Name: " + psiClass.getQualifiedName());
                classes.add(psiClass);
            }
        }
        return classes;
    }

    private PsiClass getMasterClass(PsiElement element) {
        if (null == element) {
            return null;
        }
        PsiElement context = element.getContext();
        if (context instanceof PsiClass) {
            return (PsiClass) context;
        } else {
            return getMasterClass(context);
        }
    }
}
