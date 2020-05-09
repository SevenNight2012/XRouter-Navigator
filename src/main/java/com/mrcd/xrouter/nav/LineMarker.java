package com.mrcd.xrouter.nav;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.mrcd.xrouter.nav.utils.PsiUtils;
import com.mrcd.xrouter.nav.utils.SimpleUtils;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * 入口类
 */
public class LineMarker implements LineMarkerProvider {

    private final Icon ICON_NAV = IconLoader.getIcon("/icons/nav.svg");

    @Override
    public @Nullable
    LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        String routerClassName = PsiUtils.isRouterInvoke(element);
        if (TextUtils.isEmpty(routerClassName)) {
            return null;
        }
        Project project = element.getProject();
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
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {

    }
}
