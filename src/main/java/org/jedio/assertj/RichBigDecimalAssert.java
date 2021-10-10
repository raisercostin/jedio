package org.jedio.assertj;

import java.math.BigDecimal;

import org.assertj.core.api.BigDecimalAssert;

public class RichBigDecimalAssert extends BigDecimalAssert {
  public static BigDecimalAssert assertThatBigDecimal(BigDecimal value) {
    return new RichBigDecimalAssert(value);
  }

  public RichBigDecimalAssert(BigDecimal actual) {
    super(actual);
  }

  @Override
  public BigDecimalAssert isEqualTo(Object expected) {
    objects.assertEqual(info, cleanup(actual), cleanup(expected));
    return myself;
  }

  private String cleanup(Object value) {
    if (value instanceof Integer) {
      return ((Integer) value).toString();
    }
    if (value instanceof Double) {
      return cleanup(BigDecimal.valueOf((Double) value));
    }
    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).stripTrailingZeros().toPlainString();
    }
    throw new IllegalArgumentException("Don't know how to cleanup as number an object of type " + value.getClass());
  }
}
