/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.inject.daggeradapter;

import static com.google.inject.daggeradapter.BindingSubject.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import junit.framework.TestCase;

/** Tests of {@link Binds} support in {@link DaggerAdapter}. */

public class BindsTest extends TestCase {
  @Module
  interface BasicModule {
    @Provides
    static String string() {
      return "bound";
    }

    @Binds
    CharSequence charSequence(String string);

    @Binds
    Object object(CharSequence charSequence);
  }

  public void testBinds() {
    Injector injector = Guice.createInjector(DaggerAdapter.from(BasicModule.class));
    Binding<Object> binding = injector.getBinding(Object.class);
    assertThat(binding).hasProvidedValueThat().isEqualTo("bound");
    assertThat(binding).hasSource(BasicModule.class, "object", CharSequence.class);
  }

  @Module
  static class CountingMultibindingProviderModule {
    int count = 0;

    @Provides
    String provider() {
      count++;
      return "multibound-" + count;
    }
  }

  @Module
  interface MultibindingBindsModule {
    @Binds
    @IntoSet
    Object fromString(String string);

    @Binds
    CharSequence toCharSequence(String string);

    @Binds
    @IntoSet
    Object fromCharSequence(CharSequence charSequence);
  }

  public void testMultibindings() {
    Injector injector =
        Guice.createInjector(
            DaggerAdapter.from(
                new CountingMultibindingProviderModule(), MultibindingBindsModule.class));

    Binding<Set<Object>> binding = injector.getBinding(new Key<Set<Object>>() {});
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-1", "multibound-2"));
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-3", "multibound-4"));
  }

  @Module
  interface JavaxScopedMultibindingBindsModule {
    @Binds
    @IntoSet
    @javax.inject.Singleton
    Object fromString(String string);

    @Binds
    CharSequence toCharSequence(String string);

    @Binds
    @IntoSet
    @javax.inject.Singleton
    Object fromCharSequence(CharSequence charSequence);
  }

  public void testJavaxScopedMultibindings() {
    Injector injector =
        Guice.createInjector(
            DaggerAdapter.from(
                new CountingMultibindingProviderModule(),
                JavaxScopedMultibindingBindsModule.class));

    Binding<Set<Object>> binding = injector.getBinding(new Key<Set<Object>>() {});
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-1", "multibound-2"));
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-1", "multibound-2"));
  }

  @Module
  interface JakartaScopedMultibindingBindsModule {
    @Binds
    @IntoSet
    @jakarta.inject.Singleton
    Object fromString(String string);

    @Binds
    CharSequence toCharSequence(String string);

    @Binds
    @IntoSet
    @jakarta.inject.Singleton
    Object fromCharSequence(CharSequence charSequence);
  }

  public void testJakartaScopedMultibindings() {
    Injector injector =
        Guice.createInjector(
            DaggerAdapter.from(
                new CountingMultibindingProviderModule(),
                JakartaScopedMultibindingBindsModule.class));

    Binding<Set<Object>> binding = injector.getBinding(new Key<Set<Object>>() {});
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-1", "multibound-2"));
    assertThat(binding)
        .hasProvidedValueThat()
        .isEqualTo(ImmutableSet.of("multibound-1", "multibound-2"));
  }

  @Retention(RetentionPolicy.RUNTIME)
  @javax.inject.Qualifier
  @interface JavaxProvidesQualifier {}

  @Retention(RetentionPolicy.RUNTIME)
  @javax.inject.Qualifier
  @interface JavaxBindsQualifier {}

  @Module
  interface JavaxQualifiedBinds {
    @Provides
    @JavaxProvidesQualifier
    static String provides() {
      return "qualifiers";
    }

    @Binds
    @JavaxBindsQualifier
    String bindsToProvides(@JavaxProvidesQualifier String provides);

    @Binds
    String unqualifiedToBinds(@JavaxBindsQualifier String binds);
  }

  public void testJavaxQualifiers() {
    Injector injector = Guice.createInjector(DaggerAdapter.from(JavaxQualifiedBinds.class));

    Binding<String> stringBinding = injector.getBinding(String.class);
    assertThat(stringBinding).hasProvidedValueThat().isEqualTo("qualifiers");
    assertThat(stringBinding)
        .hasSource(JavaxQualifiedBinds.class, "unqualifiedToBinds", String.class);

    Binding<String> qualifiedBinds =
        injector.getBinding(Key.get(String.class, JavaxBindsQualifier.class));
    assertThat(qualifiedBinds).hasProvidedValueThat().isEqualTo("qualifiers");
    assertThat(qualifiedBinds)
        .hasSource(JavaxQualifiedBinds.class, "bindsToProvides", String.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @jakarta.inject.Qualifier
  @interface JakartaProvidesQualifier {}

  @Retention(RetentionPolicy.RUNTIME)
  @jakarta.inject.Qualifier
  @interface JakartaBindsQualifier {}

  @Module
  interface JakartaQualifiedBinds {
    @Provides
    @JakartaProvidesQualifier
    static String provides() {
      return "jakarta qualified!";
    }

    @Binds
    @JakartaBindsQualifier
    String bindsToProvides(@JakartaProvidesQualifier String provides);

    @Binds
    String unqualifiedToBinds(@JakartaBindsQualifier String binds);
  }

  public void testJakartaQualifiers() {
    Injector injector = Guice.createInjector(DaggerAdapter.from(JakartaQualifiedBinds.class));

    Binding<String> stringBinding = injector.getBinding(String.class);
    assertThat(stringBinding).hasProvidedValueThat().isEqualTo("jakarta qualified!");
    assertThat(stringBinding)
        .hasSource(JakartaQualifiedBinds.class, "unqualifiedToBinds", String.class);

    Binding<String> qualifiedBinds =
        injector.getBinding(Key.get(String.class, JakartaBindsQualifier.class));
    assertThat(qualifiedBinds).hasProvidedValueThat().isEqualTo("jakarta qualified!");
    assertThat(qualifiedBinds)
        .hasSource(JakartaQualifiedBinds.class, "bindsToProvides", String.class);
  }
}
