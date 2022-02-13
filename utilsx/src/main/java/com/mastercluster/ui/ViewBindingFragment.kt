/*
 * Copyright (C) 2022 Leonid Belousov / mastercluster.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mastercluster.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * [ViewBindingFragment]
 *
 * A base [Fragment] class with [ViewBinding] support as a generic type parameter.
 * Eliminates the need of writing VB management boilerplate code.
 *
 * See also: [based on](https://stackoverflow.com/questions/62407823/how-using-viewbinding-with-an-abstract-base-class)
 *
 *
 * Usage in code:
 *
 * class MyFragment : ViewBindingFragment<MyFragmentBinding>() {
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *        super.onViewCreated(view, savedInstanceState)
 *
 *        binding.myButton.setOnClickListener { ... }
 *        binding.myTextView.text = "123"
 *     }
 *
 *
 * !!! MUST HAVE FOR RELEASE BUILDS !!! Add this to proguard-rules.pro:
 *
 *    -keepclassmembers, allowoptimization class ** extends androidx.viewbinding.ViewBinding {
 *        public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
 *    }
 *
 *
 * @param VB type of a [ViewBinding] class.
 * @property binding returns the correct [ViewBinding] type.
 *
 */


abstract class ViewBindingFragment<out VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View =
        createViewBindingInstance<VB>(this, inflater, container).also {
            _binding = it
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}


private fun <VB : ViewBinding> createViewBindingInstance(
    classInstance: Any, inflater: LayoutInflater, container: ViewGroup?
): VB {
    var genericViewBindingClassType: Type? = null

    // Finds in the class' hierarchy a class that has generics in its type arguments
    var klass: Class<*>? = classInstance::class.java
    search@ while (klass != null) {
        val type = klass.genericSuperclass
        klass = if (type is ParameterizedType) {
            // Returns the 1st available ViewBinding
            // on the class' generic argument list, or null if nothing found
            genericViewBindingClassType = type.actualTypeArguments.find {
                ViewBinding::class.java.isAssignableFrom(it as Class<*>)
            }

            if (genericViewBindingClassType != null) {
                break@search
            }

            // Has generics but there is no ViewBinding among them, continue searching
            type.rawType as Class<*>
        } else {
            // Has no generics, continue searching
            klass.superclass
        }
    }

    @Suppress("UNCHECKED_CAST")
    val actualViewBindingClassType = genericViewBindingClassType as? Class<VB>
        ?: throw InstantiationException("ViewBinding not found in generic arguments of the class")

    val inflate = actualViewBindingClassType.getMethod(
        "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
    )

    @Suppress("UNCHECKED_CAST")
    return inflate.invoke(null, inflater, container, false) as VB
}

