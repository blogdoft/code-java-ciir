package dev.ftathiago.ciir.application.ports;

import dev.ftathiago.ciir.application.exceptions.InvalidInputException;
import dev.ftathiago.ciir.application.model.AnalysisInput;

public interface InputResolver {

  AnalysisInput resolve(String rawPath) throws InvalidInputException;
}
