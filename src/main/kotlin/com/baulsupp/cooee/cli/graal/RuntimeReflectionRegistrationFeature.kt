package com.baulsupp.cooee.cli.graal

import com.oracle.svm.core.annotate.AutomaticFeature
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.wire.Message
import com.squareup.wire.WireEnum
import io.github.classgraph.ClassGraph
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.RuntimeReflection
import java.lang.reflect.ParameterizedType

@AutomaticFeature
internal class RuntimeReflectionRegistrationFeature : Feature {
  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    try {
      val pkg = "com.baulsupp.cooee"
      ClassGraph()
//      .verbose() // Log to stderr
        .enableClassInfo() // Scan classes, methods, fields, annotations
        .acceptPackages(pkg) // Scan com.xyz and subpackages (omit to scan all packages)
        .scan()
        .use { scanResult ->                    // Start the scan
          for (classInfo in scanResult.getSubclasses(JsonAdapter::class.java.name)) {
            registerMoshiAdapter(classInfo.loadClass())
          }             // Start the scan
          for (classInfo in scanResult.getSubclasses(Message.Builder::class.java.name)) {
            println(classInfo)
            registerWireBuilder(classInfo.loadClass())
          } // Start the scan
          for (classInfo in scanResult.getClassesImplementing(WireEnum::class.java.name)) {
            println(classInfo)
            registerWireEnum(classInfo.loadClass())
          }
        }
    } catch (e: Exception) {
      e.printStackTrace()
      throw e
    }
  }

  private fun registerMoshiAdapter(java: Class<*>) {
    RuntimeReflection.register(java)
    java.methods.forEach {
      RuntimeReflection.register(it)
    }
    val superclass = java.getGenericSuperclass() as ParameterizedType
    // extends JsonAdapter<X>()
    val valueType = superclass.actualTypeArguments.first()
    if (valueType is Class<*>) {
      valueType.constructors.forEach {
        RuntimeReflection.register(it)
      }
    }
    try {
      RuntimeReflection.register(java.getConstructor(Moshi::class.java))
    } catch (nsme: NoSuchMethodException) {
      // expected
    }
  }

  private fun registerWireBuilder(java: Class<*>) {
    RuntimeReflection.register(java)
    java.methods.forEach {
      RuntimeReflection.register(it)
    }
    java.declaredFields.forEach {
      RuntimeReflection.register(it)
    }
    RuntimeReflection.register(java.getConstructor())

    val superclass = java.getGenericSuperclass() as ParameterizedType
    // extends JsonAdapter<X>()
    val valueType = superclass.actualTypeArguments.first()
    if (valueType is Class<*>) {
      RuntimeReflection.register(valueType)
      valueType.declaredFields.forEach {
        RuntimeReflection.register(it)
      }
    }
  }

  private fun registerWireEnum(java: Class<*>) {
    RuntimeReflection.register(java)
    java.methods.forEach {
      RuntimeReflection.register(it)
    }
    java.declaredFields.forEach {
      RuntimeReflection.register(it)
    }
  }
}
