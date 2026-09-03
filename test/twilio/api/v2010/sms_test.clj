(ns twilio.api.v2010.sms-test
  (:refer-clojure :exclude [send])
  (:require [clojure.test :refer [deftest is testing]]
            [twilio.api.v2010.sms :as sms]
            [twilio.core :as core]
            [spy.core :as spy]))

(def cfg {:account-sid "AC123" :api-key "SK123" :api-secret "s"})

(def valid-msg
  {:To   "+15555550100"
   :From "+15555550199"
   :Body "hi"})

(deftest send-test
  (testing "send posts to the account's Messages endpoint with API key basic auth"
    (with-redefs [core/request (spy/stub {:sid "SM1"})]
      (is (= {:sid "SM1"} (sms/send cfg valid-msg)))
      (is (spy/called-once? core/request))
      (is (spy/called-with? core/request
                            cfg :post
                            "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json"
                            {:basic-auth  ["SK123" "s"]
                             :form-params valid-msg}))))

  (testing "custom :base-url is honored"
    (with-redefs [core/request (spy/stub {})]
      (sms/send (assoc cfg :base-url "https://example.test") valid-msg)
      (let [[[_ _ url _]] (spy/calls core/request)]
        (is (= "https://example.test/2010-04-01/Accounts/AC123/Messages.json" url)))))

  (testing "extra fields pass through (permissive schema)"
    (with-redefs [core/request (spy/stub {})]
      (let [msg (assoc valid-msg :StatusCallback "https://example.com/cb")]
        (sms/send cfg msg)
        (let [[[_ _ _ req]] (spy/calls core/request)]
          (is (= msg (:form-params req)))))))

  (testing ":MessagingServiceSid satisfies the sender requirement"
    (with-redefs [core/request (spy/stub {})]
      (sms/send cfg (-> valid-msg
                        (dissoc :From)
                        (assoc :MessagingServiceSid "MG123")))
      (is (spy/called-once? core/request))))

  (testing "throws ex-info when both :From and :MessagingServiceSid are missing"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (sms/send cfg (dissoc valid-msg :From))))
      (is (spy/not-called? core/request))))

  (testing "throws ex-info on missing required fields"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (sms/send cfg {})))
      (is (spy/not-called? core/request))))

  (testing "throws ex-info on invalid cfg"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (sms/send {:account-sid "AC123" :api-key "SK123"} valid-msg)))
      (is (spy/not-called? core/request)))))
