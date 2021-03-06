(ns rhsm.gui.tasks.tools
  (:use [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           cli-tasks)]
        [slingshot.slingshot :only [throw+ try+]]
        [clojure.string :only (trim
                               split
                               join)])
  (:require [clojure.tools.logging :as log])
  (:import [com.redhat.qe.tools RemoteFileTasks]
               org.testng.SkipException
               [com.redhat.qe.auto.bugzilla BzChecker]))

(defn run-command
  "Runs a given command on the client using SSHCommandRunner()."
  [command & {:keys [runner]
                              :or {runner @clientcmd}}]
  (let [result (.runCommandAndWait runner command)
         out (.getStdout result)
         err (.getStderr result)
         exit (.getExitCode result)]
     {:stdout out
      :stderr err
      :exitcode exit}))

(defn get-release
  "Returns a key representing the RHEL release on the client."
  ([verbose?]
     (let [release (:stdout (run-command "cat /etc/redhat-release"))
           fm (re-find #"release \d" release)
           family (case fm
                    "release 5" "RHEL5"
                    "release 6" "RHEL6"
                    "release 7" "RHEL7"
                    :unknown)
           variant (join " " (take-while #(not= "release" %) (drop 4 (split release #" "))))
           version (re-find #"\d.\d+" release)]
       (if verbose?
         {:family family
          :variant variant
          :version version}
         family)))
  ([] (get-release false)))

(defn sleep
  "Sleeps for a given ammount of miliseconds."
  [ms]
  (. Thread (sleep ms)))

(defn bash-bool [i]
  (= 0 i))

(defn bool [i]
  (= 1 i))

(def is-boolean?
  (fn [expn]
    (or
      (= expn 'true)
      (= expn 'false))))

(def not-nil? (fn [b] (not (nil? b))))

(defmacro loop-timeout [timeout bindings & forms]
  `(let [starttime# (System/currentTimeMillis)]
     (loop ~bindings
       (if  (> (- (System/currentTimeMillis) starttime#) ~timeout)
         (throw (RuntimeException. (str "Hit timeout of " ~timeout "ms.")))
         (do ~@forms)))))

(defn #^String substring?
  "True if s contains the substring."
  [substring #^String s]
  (.contains s substring))

(defn get-default-locale
  [] (trim (:stdout (run-command "echo $LANG"))))

(defn get-locale-regex
  "Gets the correct formated locale date string and transforms it into a usable regex.
    @locale : The locale to use eg: 'en_US' (optional)"
  ([locale]
     (let [cmd (str "python -c \"import locale; locale.setlocale(locale.LC_TIME,'"
                    locale
                    "'); print locale.nl_langinfo(locale.D_FMT)\"")
           pyformat (clojure.string/trim (:stdout (run-command cmd)))
           transform (fn [s] (cond (= s "%d") "\\\\d{2}"
                                  (= s "%m") "\\\\d{2}"
                                  (= s "%y") "\\\\d{2}"
                                  (= s "%Y") "\\\\d{4}"))]
       (re-pattern (clojure.string/replace pyformat #"%\w{1}" #(transform %1)))))
  ([]
     (get-locale-regex (get-default-locale))))

(defmacro get-logging [runner logfile name grep & forms]
  "Runs given forms and returns changes to a log file.
  runner: an instance of a SSHCommandRunner
  log: string containing which log file to look at
  name: string used to mark the log file
  grep: what to grep the results for"
  `(let [marker# (str (System/currentTimeMillis) " " ~name)]
     (RemoteFileTasks/markFile ~runner ~logfile marker#)
     (do ~@forms)
     (RemoteFileTasks/getTailFromMarkedFile ~runner ~logfile marker# ~grep)))

(defmacro skip-if-bz-open [bugid & forms]
  `(let [bz# (BzChecker/getInstance)
         open?# (.isBugOpen bz# ~bugid)
         state# (str (.getBugState bz# ~bugid))
         summary# (.getBugField bz# ~bugid "summary")]
     (if (and open?#
              ~@forms)
       (throw (SkipException. (str "This test is blocked by "
                                   state#
                                   " Bugzilla bug '"
                                   summary#
                                   "'.  (https://bugzilla.redhat.com/show_bug.cgi?id="
                                   ~bugid
                                   ")"))))))
