/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.analysis.reflection;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.jf.dexlib2.analysis.reflection.util.ReflectionUtils;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Wraps a ClassDef around a class loaded in the current VM
 *
 * Only supports the basic information exposed by ClassProto
 */
public class ReflectionClassDef extends BaseTypeReference implements ClassDef {
    private final Class cls;

    public ReflectionClassDef(Class cls) {
        this.cls = cls;
    }

    @Override public int getAccessFlags() {
        // the java modifiers appear to be the same as the dex access flags
        return cls.getModifiers();
    }

    @Nullable @Override public String getSuperclass() {
        if (Modifier.isInterface(cls.getModifiers())) {
            return "Ljava/lang/Object;";
        }
        Class superClass = cls.getSuperclass();
        if (superClass == null) {
            return null;
        }
        return ReflectionUtils.javaToDexName(superClass.getName());
    }

    @Nonnull @Override public Set<String> getInterfaces() {
        return new AbstractSet<String>() {
            @Nonnull @Override public Iterator<String> iterator() {
                return Iterators.transform(Iterators.forArray(cls.getInterfaces()), new Function<Class, String>() {
                    @Nullable @Override public String apply(@Nullable Class input) {
                        if (input == null) {
                            return null;
                        }
                        return ReflectionUtils.javaToDexName(input.getName());
                    }
                });
            }

            @Override public int size() {
                return cls.getInterfaces().length;
            }
        };
    }

    @Nullable @Override public String getSourceFile() {
        return null;
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        return ImmutableSet.of();
    }

    @Nonnull @Override public Set<? extends Field> getFields() {
        return new AbstractSet<Field>() {
            @Nonnull @Override public Iterator<Field> iterator() {
                return Iterators.transform(Iterators.forArray(cls.getDeclaredFields()),
                        new Function<java.lang.reflect.Field, Field>() {
                            @Nullable @Override public Field apply(@Nullable java.lang.reflect.Field input) {
                                return new ReflectionField(input);
                            }
                        });
            }

            @Override public int size() {
                return cls.getDeclaredFields().length;
            }
        };
    }

    @Nonnull @Override public Set<? extends Method> getMethods() {
        return new AbstractSet<Method>() {
            @Nonnull @Override public Iterator<Method> iterator() {
                Iterator<Method> constructorIterator =
                        Iterators.transform(Iterators.forArray(cls.getDeclaredConstructors()),
                                new Function<Constructor, Method>() {
                                    @Nullable @Override public Method apply(@Nullable Constructor input) {
                                        return new ReflectionConstructor(input);
                                    }
                                });

                Iterator<Method> methodIterator =
                        Iterators.transform(Iterators.forArray(cls.getDeclaredMethods()),
                                new Function<java.lang.reflect.Method, Method>() {
                                    @Nullable @Override public Method apply(@Nullable java.lang.reflect.Method input) {
                                        return new ReflectionMethod(input);
                                    }
                                });
                return Iterators.concat(constructorIterator, methodIterator);
            }

            @Override public int size() {
                return cls.getDeclaredMethods().length + cls.getDeclaredConstructors().length;
            }
        };
    }

    @Nonnull @Override public String getType() {
        return ReflectionUtils.javaToDexName(cls.getName());
    }
}