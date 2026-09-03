(ns twilio.api.v1.email-test
  (:refer-clojure :exclude [send])
  (:require [clojure.test :refer [deftest is testing]]
            [twilio.api.v1.email :as email]
            [twilio.core :as core]
            [spy.core :as spy]))

(def cfg {:api-key "SK123" :api-secret "s"})

(def valid-body
  {:from    {:address "noreply@example.com" :name "Example"}
   :to      [{:address "user@example.com"}]
   :content {:subject "Hello"
             :html    "<p>Hi there</p>"}})

(def accepted
  {:operationId       "comms_operation_1"
   :operationLocation "https://comms.twilio.com/v1/Emails/Operations/comms_operation_1"})

(deftest send-test
  (testing "send posts to /v1/Emails with API key basic auth"
    (with-redefs [core/request (spy/stub accepted)]
      (is (= accepted (email/send cfg valid-body)))
      (is (spy/called-once? core/request))
      (is (spy/called-with? core/request
                            cfg :post
                            "https://comms.twilio.com/v1/Emails"
                            {:basic-auth   ["SK123" "s"]
                             :form-params  valid-body
                             :content-type :json}))))

  (testing "custom :base-url is honored"
    (with-redefs [core/request (spy/stub accepted)]
      (email/send (assoc cfg :base-url "https://example.test") valid-body)
      (let [[[_ _ url _]] (spy/calls core/request)]
        (is (= "https://example.test/v1/Emails" url)))))

  (testing "extra fields pass through (permissive schema)"
    (with-redefs [core/request (spy/stub accepted)]
      (let [body (assoc valid-body :tags {:campaign "welcome"})]
        (email/send cfg body)
        (let [[[_ _ _ req]] (spy/calls core/request)]
          (is (= body (:form-params req)))))))

  (testing "throws ex-info on missing required fields"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (email/send cfg (dissoc valid-body :content))))
      (is (spy/not-called? core/request))))

  (testing "throws ex-info when :from lacks :name"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (email/send cfg (update valid-body :from dissoc :name))))
      (is (spy/not-called? core/request))))

  (testing "throws ex-info when :content lacks :html"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (email/send cfg (update valid-body :content dissoc :html))))
      (is (spy/not-called? core/request))))

  (testing "throws ex-info on invalid cfg"
    (with-redefs [core/request (spy/stub nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid input"
                            (email/send {} valid-body)))
      (is (spy/not-called? core/request)))))

(deftest simple-body-test
  (testing "builds the Twilio Email shape from a flat map"
    (is (= valid-body
           (email/simple-body {:from      "noreply@example.com"
                               :from-name "Example"
                               :to        "user@example.com"
                               :subject   "Hello"
                               :html      "<p>Hi there</p>"}))))

  (testing "includes :text when given, omits it when nil"
    (let [body (email/simple-body {:from "a@b.com" :from-name "n" :to "c@d.com" :subject "s"
                                   :html "<p>hi</p>" :text "hi"})]
      (is (= {:subject "s" :html "<p>hi</p>" :text "hi"} (:content body))))
    (let [body (email/simple-body {:from "a@b.com" :from-name "n" :to "c@d.com" :subject "s"
                                   :html "<p>hi</p>"})]
      (is (not (contains? (:content body) :text))))))
