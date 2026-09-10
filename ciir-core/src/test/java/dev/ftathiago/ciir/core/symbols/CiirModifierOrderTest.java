package dev.ftathiago.ciir.core.symbols;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CiirModifierOrderTest {

  @Test
  void sort_reordersRegardlessOfInputOrder() {
    var sorted = CiirModifierOrder.sort(List.of(CiirModifier.FINAL, CiirModifier.STATIC));

    assertThat(sorted).containsExactly(CiirModifier.STATIC, CiirModifier.FINAL);
  }

  @Test
  void sort_dropsDuplicates() {
    var sorted = CiirModifierOrder.sort(List.of(CiirModifier.STATIC, CiirModifier.STATIC));

    assertThat(sorted).containsExactly(CiirModifier.STATIC);
  }

  @Test
  void sort_projectsCanonicalOrderDownToWhatIsPresent() {
    var sorted =
        CiirModifierOrder.sort(
            List.of(CiirModifier.DEFAULT, CiirModifier.ABSTRACT, CiirModifier.STATIC));

    assertThat(sorted)
        .containsExactly(CiirModifier.STATIC, CiirModifier.ABSTRACT, CiirModifier.DEFAULT);
  }

  @Test
  void sort_emptyInputProducesEmptyOutput() {
    assertThat(CiirModifierOrder.sort(List.of())).isEmpty();
  }
}
