package com.tersesystems.blacklite.archive;

public interface ArchiveResult {
  class Success implements ArchiveResult {
    private final int archived;

    public Success(int archived) {
      this.archived = archived;
    }

    public int getArchived() {
      return archived;
    }

    @Override
    public String toString() {
      return "Success(" + archived + ")";
    }
  }

  class NoOp implements ArchiveResult {
    public static final NoOp instance = new NoOp();

    @Override
    public String toString() {
      return "NoOp()";
    }
  }

  class Failure implements ArchiveResult {
    private final Exception exception;

    public Failure(Exception e) {
      this.exception = e;
    }

    public Exception getException() {
      return exception;
    }

    @Override
    public String toString() {
      return "Failure(" + exception + ")";
    }
  }
}
