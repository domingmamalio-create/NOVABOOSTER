package com.drayvokz.booster

import android.app.ActivityManager
import android.content.Context
import java.io.File

object DeviceOptimizer {

    fun killBackground(ctx: Context): Int {
        var killed = 0
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = ctx.packageManager
            val pkgs = pm.getInstalledPackages(0)
            val myPkg = ctx.packageName
            for (p in pkgs) {
                val pkgName = p.packageName
                if (pkgName == myPkg || pkgName == "android") continue
                try {
                    am.killBackgroundProcesses(pkgName)
                    killed++
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return killed
    }

    fun clearCache(ctx: Context): Long {
        var freed = 0L
        try {
            val dirs = listOfNotNull(
                ctx.cacheDir,
                ctx.externalCacheDir,
                ctx.filesDir.parentFile?.let { File(it, "cache") }
            )
            for (d in dirs) {
                if (d.exists()) freed += deleteRecursive(d)
            }
        } catch (_: Exception) {}
        return freed
    }

    private fun deleteRecursive(f: File): Long {
        var total = 0L
        try {
            if (f.isDirectory) f.listFiles()?.forEach { total += deleteRecursive(it) }
            else { total += f.length(); f.delete() }
        } catch (_: Exception) {}
        return total
    }

    fun ramInfo(ctx: Context): Pair<Long, Long> {
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            ((mi.totalMem - mi.availMem) / 1048576) to (mi.totalMem / 1048576)
        } catch (_: Exception) { 0L to 0L }
    }
}
