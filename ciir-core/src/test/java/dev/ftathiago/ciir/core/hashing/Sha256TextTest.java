package dev.ftathiago.ciir.core.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256TextTest {

  @Test
  void computeHash_isDeterministic() {
    assertThat(Sha256Text.computeHash("hello")).isEqualTo(Sha256Text.computeHash("hello"));
  }

  @Test
  void computeHash_isLowercaseHexOfKnownValue() {
    // echo -n "hello" | sha256sum
    assertThat(Sha256Text.computeHash("hello"))
        .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  }

  @Test
  void computePrefixedHash_hasSha256Prefix() {
    assertThat(Sha256Text.computePrefixedHash("hello")).startsWith("sha256:");
  }

  @Test
  void computeHash_differentInputsProduceDifferentHashes() {
    assertThat(Sha256Text.computeHash("hello")).isNotEqualTo(Sha256Text.computeHash("world"));
  }
}
