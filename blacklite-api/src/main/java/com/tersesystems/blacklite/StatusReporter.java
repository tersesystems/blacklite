package com.tersesystems.blacklite;

public interface StatusReporter {

  StatusReporter DEFAULT =
      new StatusReporter() {
        public void addInfo(String message) {
          System.out.println(message);
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
          System.out.println(msg);
          ex.printStackTrace(System.out);
        }

        @Override
        public void addWarn(String msg) {
          System.out.println(msg);
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
          System.err.println(msg);
          ex.printStackTrace(System.err);
        }

        @Override
        public void addError(String msg) {
          System.err.println(msg);
        }

        public void addError(String msg, Throwable e) {
          System.err.println(msg);
          e.printStackTrace();
        }
      };

  void addInfo(String msg);

  void addInfo(String msg, Throwable ex);

  void addWarn(String msg);

  void addWarn(String msg, Throwable ex);

  void addError(String msg);

  void addError(String msg, Throwable ex);
}
