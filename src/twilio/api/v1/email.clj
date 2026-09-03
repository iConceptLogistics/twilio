(ns twilio.api.v1.email
  "Wraps Twilio Email's v1 Emails API (comms.twilio.com).

  `send` takes the raw Twilio Email request shape; `simple-body` builds
  that shape from a flat map for the common single-recipient case.
  Schemas are permissive (`:closed false`) so any extra Twilio-supported
  fields pass through untouched (e.g. `:schedule`, `:tags`, per-recipient
  `:variables`)."
  (:refer-clojure :exclude [send])
  (:require [twilio.core :as core]))

(def default-base-url "https://comms.twilio.com")

(def SendSchema
  [:map {:closed false}
   [:from [:map {:closed false}
           [:address :string]
           [:name    :string]]]
   [:to [:sequential [:map {:closed false} [:address :string]]]]
   [:content
    [:map {:closed false}
     [:subject :string]
     [:html    :string]]]])

(defn simple-body
  "Build a Twilio Email send body from a flat map.

  Required keys: :from, :from-name, :to, :subject, :html. Optional: :text
  (Twilio generates a plain-text part from :html when omitted)."
  [{:keys [from from-name to subject html text]}]
  {:from    {:address from :name from-name}
   :to      [{:address to}]
   :content (cond-> {:subject subject
                     :html    html}
              text (assoc :text text))})

(defn send
  "Send an email via Twilio Email.

  `cfg`  - {:api-key :api-secret & {:base-url :http-opts}} (see twilio.core/CfgSchema)
  `body` mirrors Twilio Email's request shape (see `simple-body` for a helper):
    {:from    {:address \"noreply@example.com\" :name \"Example\"}
     :to      [{:address \"user@example.com\"}]
     :content {:subject \"Welcome\" :html \"<p>Hi there</p>\"}}

  Sends are asynchronous: Twilio responds 202 with {:operationId ...
  :operationLocation ...}; GET the operationLocation to poll send status."
  [cfg body]
  (core/validate! core/CfgSchema cfg)
  (core/validate! SendSchema body)
  (let [{:keys [api-key api-secret base-url]
         :or   {base-url default-base-url}} cfg]
    (core/request cfg :post
                  (str base-url "/v1/Emails")
                  {:basic-auth   [api-key api-secret]
                   :form-params  body
                   :content-type :json})))
