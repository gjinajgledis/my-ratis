package org.example.myratis.common.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A reference-counted object can be retained for later use
 * and then be released for returning the resource.
 *
 * - When the object is retained, the reference count is incremented by 1.
 *
 * - When the object is released, the reference count is decremented by 1.
 *
 * - If the object is retained, it must be released afterward.
 *   Otherwise, the object will not be returned, and it will cause a resource leak.
 *
 * - If the object is retained multiple times,
 *   it must be released the same number of times.
 *
 * - If the object has been retained and then completely released (i.e. the reference count becomes 0),
 *   it must not be retained/released/accessed anymore since it may have been allocated for other use.
 *
 * @param <T> The object type.
 */
public interface ReferenceCountedObject<T> {
  /** @return the object. */
  T get();

  /**
   * Retain the object for later use.
   * The reference count will be increased by 1.
   *
   * The {@link #release()} method must be invoked afterward.
   * Otherwise, the object is not returned, and it will cause a resource leak.
   *
   * @return the object.
   */
  T retain();

  /**
   * Release the object.
   * The reference count will be decreased by 1.
   *
   * @return true if the object is completely released (i.e. reference count becomes 0); otherwise, return false.
   */
  boolean release();

  /**
   * Wrap the given value as a {@link ReferenceCountedObject}.
   *
   * @param value the value being wrapped.
   * @param retainMethod a method to run when {@link #retain()} is invoked.
   * @param releaseMethod a method to run when {@link #release()} is invoked.
   * @param <V> The value type.
   * @return the wrapped reference-counted object.
   */
  static <V> ReferenceCountedObject<V> wrap(V value, Runnable retainMethod, Runnable releaseMethod) {
    Objects.requireNonNull(value, "value == null");
    Objects.requireNonNull(retainMethod, "retainMethod == null");
    Objects.requireNonNull(releaseMethod, "releaseMethod == null");

    return new ReferenceCountedObject<V>() {
      private final AtomicInteger count = new AtomicInteger();

      @Override
      public V get() {
        if (count.get() < 0) {
          throw new IllegalStateException("Failed to get: object has already been completely released.");
        }
        return value;
      }

      @Override
      public V retain() {
        // n <  0: exception
        // n >= 0: n++
        if (count.getAndUpdate(n -> n < 0? n : n + 1) < 0) {
          throw new IllegalStateException("Failed to retain: object has already been completely released.");
        }

        retainMethod.run();
        return value;
      }

      @Override
      public boolean release() {
        // n <= 0: exception
        // n >  1: n--
        // n == 1: n = -1
        final int previous = count.getAndUpdate(n -> n <= 1? -1: n - 1);
        if (previous < 0) {
          throw new IllegalStateException("Failed to release: object has already been completely released.");
        } else if (previous == 0) {
          throw new IllegalStateException("Failed to release: object has not yet been retained.");
        }
        releaseMethod.run();
        return previous == 1;
      }
    };
  }
}