package com.example.foliatemplate.addon

import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * One classloader per addon jar.
 *
 * The parent is the HOST's classloader, so the addon's `Addon`, `AddonContext`,
 * etc. resolve to the *same class objects* the host uses. That matters: if an
 * addon bundled its own copy of the API, `instanceof Addon` would fail even
 * though the names match (the classic "same name, different classloader" bug).
 * Addons must therefore declare the addon-api as `compileOnly`, never shade it.
 *
 * Addon-private dependencies bundled inside the addon jar still resolve here,
 * since we check our own URLs before delegating for anything not found in the
 * parent.
 */
class AddonClassLoader(
    jar: File,
    parent: ClassLoader,
) : URLClassLoader(arrayOf<URL>(jar.toURI().toURL()), parent)
