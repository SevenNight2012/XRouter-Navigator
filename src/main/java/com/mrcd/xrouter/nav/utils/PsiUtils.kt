package com.mrcd.xrouter.nav.utils

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.LightClassUtil.getLightClassMethod
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.util.regex.Pattern

object PsiUtils {

    @JvmStatic
    fun getClass(psiType: PsiType?, ele: PsiElement): PsiClass? {
        if (psiType is PsiClassType) {
            return psiType.resolve()
        } else if (psiType is PsiPrimitiveType) {
            return psiType.getBoxedType(ele)!!.resolve()
        }
        return null
    }

    @JvmStatic
    fun isRouterInvoke(psiElement: PsiElement): String? {
        // pkg.class.method
        val fullName: String? = when (psiElement) {
            is PsiCallExpression -> psiElement.resolveMethod()?.fullName
            is KtNameReferenceExpression -> psiElement.resolve()?.fullName
            else -> ""
        }
        fullName ?: return ""

//        System.err.println(psiElement.text + "  >>>>  " + fullName + "   " + psiElement.javaClass.name)
        var className: String? = null
        if (Pattern.matches(".*Router().*\\.launch()", fullName)) {
            val dotIndex = fullName.lastIndexOf(".")
            className = fullName.substring(0, dotIndex)
            println(className)
        }
        return className
    }

}

fun PsiMethod.firstParamTypeName(): String? {
    return PsiUtils.getClass(parameters[0].type as PsiType, this)?.qualifiedName
}

val PsiElement.fullName: String? get() = getKotlinFqName()?.asString()

//kotlinInternalUastUtils
//https://github.com/JetBrains/kotlin/blob/master/plugins/uast-kotlin/src/org/jetbrains/uast/kotlin/internal/kotlinInternalUastUtils.kt
fun KtNamedFunction.toPsiMethod(): PsiMethod? = getLightClassMethod(this)

/**
 * 获取Kt 函数参数
 */
fun KtNamedFunction.firstParamType(): String? {
    //参数类型
    return when (val pType = toPsiMethod()?.parameterList?.parameters?.getOrNull(0)?.type) {
        //基本类型
        is PsiPrimitiveType -> pType.boxedTypeName
        else -> pType?.canonicalText
    }
}

fun String.isKotPrimitiveType(): Boolean {
    return this in KotPrimitiveType2JavaWrap.keys
}

private val KotPrimitiveType2JavaWrap
    get() = mapOf(
            "kotlin.Int" to "java.lang.Integer",
            "kotlin.Boolean" to "java.lang.Boolean",
            "kotlin.Char" to "java.lang.Character",
            "kotlin.Double" to "java.lang.Double",
            "kotlin.Float" to "java.lang.Float",
            "kotlin.Long" to "java.lang.Long",
            "kotlin.Any" to "java.lang.Object",
            "kotlin.String" to "java.lang.String"
    )

//kt cls -> java cls
fun String.toJavaType(): String {
    return KotPrimitiveType2JavaWrap.getOrDefault(this, this)
}
