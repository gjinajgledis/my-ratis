/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.myratis.common.util;


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * General Java utility methods.
 */
public interface JavaUtils {
  CompletableFuture<?>[] EMPTY_COMPLETABLE_FUTURE_ARRAY = {};

  ConcurrentMap<Class<?>, String> CLASS_SIMPLE_NAMES = new ConcurrentHashMap<>();
  static String getClassSimpleName(Class<?> clazz) {
    return CLASS_SIMPLE_NAMES.computeIfAbsent(clazz, Class::getSimpleName);
  }

  static String date() {
    return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS").format(new Date());
  }

  /**
   * The same as {@link Class#cast(Object)} except that
   * this method returns null (but not throw {@link ClassCastException})
   * if the given object is not an instance of the given class.
   */
  static <T> T cast(Object obj, Class<T> clazz) {
    return clazz.isInstance(obj)? clazz.cast(obj): null;
  }

  static <T> T cast(Object obj) {
    @SuppressWarnings("unchecked")
    final T t = (T)obj;
    return t;
  }

  static StackTraceElement getCallerStackTraceElement() {
    final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    return trace[3];
  }

  static StackTraceElement getCurrentStackTraceElement() {
    final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    return trace[2];
  }


  /**
   * Create a memoized supplier which gets a value by invoking the initializer once
   * and then keeps returning the same value as its supplied results.
   *
   * @param initializer to supply at most one non-null value.
   * @param <T> The supplier result type.
   * @return a memoized supplier which is thread-safe.
   */
  static <T> MemoizedSupplier<T> memoize(Supplier<T> initializer) {
    return MemoizedSupplier.valueOf(initializer);
  }

  Supplier<ThreadGroup> ROOT_THREAD_GROUP = memoize(() -> {
    for (ThreadGroup g = Thread.currentThread().getThreadGroup(), p;; g = p) {
      if ((p = g.getParent()) == null) {
        return g;
      }
    }
  });

  static ThreadGroup getRootThreadGroup() {
    return ROOT_THREAD_GROUP.get();
  }


  static Timer runRepeatedly(Runnable runnable, long delay, long period, TimeUnit unit) {
    final Timer timer = new Timer(true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        runnable.run();
      }
    }, unit.toMillis(delay), unit.toMillis(period));

    return timer;
  }

  static void dumpAllThreads(Consumer<String> println) {
    final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo ti : threadMxBean.dumpAllThreads(true, true)) {
      println.accept(ti.toString());
    }
  }

  static <E> CompletableFuture<E> completeExceptionally(Throwable t) {
    final CompletableFuture<E> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }

  static Throwable unwrapCompletionException(Throwable t) {
    Objects.requireNonNull(t, "t == null");
    return t instanceof CompletionException && t.getCause() != null? t.getCause(): t;
  }

  static <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> futures) {
    return CompletableFuture.allOf(futures.toArray(EMPTY_COMPLETABLE_FUTURE_ARRAY));
  }

  static <V> BiConsumer<V, Throwable> asBiConsumer(CompletableFuture<V> future) {
    return (v, t) -> {
      if (t != null) {
        future.completeExceptionally(t);
      } else {
        future.complete(v);
      }
    };
  }

}