package com.oltpbenchmark.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper Class for MQL (Mongo Query Language) Statements. */
public final class MQLStmt {
  private static final Logger LOG = LoggerFactory.getLogger(MQLStmt.class);
  private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\?\\?");

  private String originalQuery;
  private String query;
  private final int[] substitutions;

  public MQLStmt(String query, int... substitutions) {
    this.substitutions = substitutions;
    this.setQuery(query);
  }

  public final void setQuery(String query) {
    this.originalQuery = query.trim();
    for (int count : this.substitutions) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < count; i++) {
        if (i > 0) sb.append(", ");
        sb.append("?");
      }
      Matcher m = SUBSTITUTION_PATTERN.matcher(query);
      query = m.replaceFirst(sb.toString());
    }
    this.query = query;

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized MQL:\n{}", this.query);
    }
  }

  public final String getQuery() {
    return this.query;
  }

  protected final String getOriginalQuery() {
    return this.originalQuery;
  }

  @Override
  public String toString() {
    return "MQLStmt{" + this.query + "}";
  }
}
