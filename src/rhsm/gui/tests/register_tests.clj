(ns rhsm.gui.tests.register_tests
  (:use [test-clj.testng :only [gen-class-testng
                                data-driven]]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [clojure.string :only (blank?)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks])
  (:import [org.testng.annotations
            Test
            BeforeClass
            DataProvider]))

(def sys-log "/var/log/rhsm/rhsm.log")

(defn get-userlists [username password]
  (let [owners (ctasks/get-owners username password)]
    (for [owner owners] (vector username password owner))))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (try+ (tasks/unregister)
        (catch [:type :not-registered] _)))

(defn ^{Test {:groups ["registration"
                       "acceptance"]
              :dataProvider "userowners"}}
  simple_register
  "Simple register with known username, password and owner."
  [_ user pass owner]
  (try+
   (if owner
     (tasks/register user pass :owner owner)
     (tasks/register user pass))
   (catch [:type :already-registered]
       {:keys [unregister-first]} (unregister-first)))
  (verify (not (tasks/ui showing? :register-system)))
  (if owner
    (try
      (do
        (tasks/ui click :view-system-facts)
        (tasks/ui waittillwindowexist :facts-dialog 10)
        (verify (= owner (tasks/ui gettextvalue :facts-org))))
      (finally (if (bool (tasks/ui guiexist :facts-dialog))
                 (tasks/ui click :close-facts))))))

(defn register_bad_credentials
  "Checks error messages upon registering with bad credentials."
  [user pass recovery]
  (try+ (tasks/unregister) (catch [:type :not-registered] _))
  (let [test-fn (fn [username password expected-error-type]
                  (try+ (tasks/register username password)
                        (catch
                            [:type expected-error-type]
                            {:keys [type cancel]}
                          (cancel) type)))]
    (let [thrown-error (apply test-fn [user pass recovery])
          expected-error recovery
          register-button :register-system]
     (verify (and (= thrown-error expected-error) (action exists? register-button))))))

(defn ^{Test {:groups ["registration"
                       "acceptance"]}}
  unregister
  "Simple unregister."
  [_]
  (try+ (tasks/register (@config :username) (@config :password))
        (catch
            [:type :already-registered]
            {:keys [unregister-first]} (unregister-first)))
  (tasks/unregister)
  (verify (action exists? :register-system)))

(defn ^{Test {:groups ["registration"
                       "blockedByBug-918303"]
              :priority (int 10)}}
  register_check_syslog
  "Asserts that register events are logged in the syslog."
  [_]
  (let [output (get-logging @clientcmd
                                  sys-log
                                  "register_check_syslog"
                                  nil
                                  (tasks/register-with-creds))]
      (verify (not (blank? output)))))

(defn ^{Test {:groups ["registration"
                       "blockedByBug-918303"]
              :dependsOnMethods ["register_check_syslog"]
              :priority (int 20)}}
  unregister_check_syslog
  "Asserts unregister events are logged in the syslog."
  [_]
  ;(tasks/register-with-creds)
  (let [output (get-logging @clientcmd
                                  sys-log
                                  "unregister_check_syslog"
                                  nil
                                  (tasks/unregister))]
      (verify (not (blank? output)))))

(data-driven register_bad_credentials {Test {:groups ["registration"]}}
  [^{Test {:groups ["blockedByBug-718045"]}}
   ["sdf" "sdf" :invalid-credentials]
   ;need to add a case with a space in the middle re: 719378
   ;^{Test {:groups ["blockedByBug-719378"]}}
   ;["test user" :invalid-credentials]
   ["" "" :no-username]
   ["" "password" :no-username]
   ["sdf" "" :no-password]])

(defn ^{Test {:groups ["registration"
                       "blockedByBug-822706"]
              ;:dependsOnMethods ["simple_register"]
              }}
  check_auto_to_register_button
  "Checks that the register button converts to the auto-subscribe button after register."
  [_]
  (tasks/restart-app :unregister? true)
  (verify (and (tasks/ui showing? :register-system)
               (not (tasks/ui showing? :auto-attach))))
  (tasks/register-with-creds)
  (verify (and (tasks/ui showing? :auto-attach)
               (not (tasks/ui showing? :register-system)))))

(defn ^{Test {:groups ["registration"
                       "blockedByBug-878609"]}}
verify_password_tip
"Checks to see if the passeword tip no longer contains red.ht"
[_]
(if (not (tasks/ui showing? :register-system))
  (tasks/unregister))
(tasks/ui click :register-system)
(tasks/ui waittillguiexist :register-dialog)
(tasks/ui click :register)
(try
  (let [tip (tasks/ui gettextvalue :password-tip)]
    (verify (not (substring? "red.ht" tip))))
  (finally
    (tasks/ui click :register-cancel))))

(defn ^{Test (:groups ["registration"
                       "blockedByBug-920091"])}
  check_traceback_unregister
  "checks for traceback if any during unregister with GUI open"
  [_]
  (if (not (tasks/ui showing? :register-system))
  (tasks/unregister))
  (let [log "/var/log/rhsm/rhsm.log"
        output (get-logging @clientcmd
                            log
                            "Traceback-register-unregister-GUI-open"
                            nil
                            (run-command (str  "subscription-manager register --user=" (@config :username) " --password=" (@config :password) " --org=" (@config :owner-key) " --auto-attach"))
                            (run-command "subscription-manager unregister"))]
    (verify (not (substring? "Traceback" output)))))

;;;;;;;;;;;;;;;;;;;;
;; DATA PROVIDERS ;;
;;;;;;;;;;;;;;;;;;;;

(defn ^{DataProvider {:name "userowners"}}
  get_userowners [_ & {:keys [debug]
                       :or {debug false}}]
  (let [data (vec
              (conj
               (into
                (if (and (@config :username1) (@config :password1))
                  (get-userlists (@config :username1) (@config :password1)))
                (if (and (@config :username) (@config :password))
                  (get-userlists (@config :username) (@config :password))))
               ; https://bugzilla.redhat.com/show_bug.cgi?id=719378
               (if (and (@config :username) (@config :password))
                 [(str (@config :username) "   ") (@config :password) nil])
               ; https://bugzilla.redhat.com/show_bug.cgi?id=719378
               (if (and (@config :username) (@config :password))
                 [(str "   " (@config :username)) (@config :password) nil])))]
    (if-not debug
      (to-array-2d data)
      data)))

(gen-class-testng)


(comment  ;now using testNG dataproviders
(data-driven simple_register {Test {:groups ["registration"]}}
  [[(@config :username) (@config :password) "Admin Owner"]
   [(@config :username) (@config :password) "Snow White"]
   [(@config :username1) (@config :password1) nil]
   ^{Test {:groups ["blockedByBug-719378"]}}
   [(str (@config :username) "   ") (@config :password) nil]
   ^{Test {:groups ["blockedByBug-719378"]}}
   [(str "   " (@config :username)) (@config :password) nil]])
)
