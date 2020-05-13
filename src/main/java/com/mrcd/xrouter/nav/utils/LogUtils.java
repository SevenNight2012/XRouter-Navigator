package com.mrcd.xrouter.nav.utils;

/**
 * LogUtils
 */
public class LogUtils {

    private static boolean sCanLog = true;

    private LogUtils() {

    }

    /**
     * 获取当前代码所在类及行号
     *
     * @return 自定义tag
     */
    private static String tag() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
        String tag = "%s.%s(L:%d)";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(), caller.getLineNumber());
        return tag;
    }

    public static void d(String msg) {
        if (sCanLog) {
            System.out.println(tag() + " > " + msg);
        }
    }

    public static void e(String msg) {
        if (sCanLog) {
            System.err.println(tag() + " > " + msg);
        }
    }

}
