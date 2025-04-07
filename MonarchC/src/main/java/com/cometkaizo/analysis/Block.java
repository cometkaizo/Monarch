package com.cometkaizo.analysis;

import com.cometkaizo.parser.Structure;

import java.util.List;

/**
 * Indicates that this analysis object contains a sequence of statements
 */
public interface Block {
    List<? extends Structure.Analysis> statements();
}
