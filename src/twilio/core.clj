(ns twilio.core
  "Shared HTTP plumbing for the Twilio API wrappers.

  Both products (SMS on api.twilio.com, email on comms.twilio.com)
  authenticate with the same Twilio API Key SID/Secret pair via Basic
  auth, so the shared credential schema lives here; SMS additionally
  needs the Account SID for its URL path."
  (:require [clj-http.client :as http]
            [malli.core :as m]
            [malli.error :as me]))

(def CfgSchema
  [:map {:closed false}
   [:api-key    :string]
   [:api-secret :string]
   [:base-url {:optional true} :string]])

(def default-http-opts
  {:socket-timeout     10000
   :connection-timeout 5000
   :throw-exceptions   false
   :as                 :json
   :coerce             :always})

(defn validate!
  "Validate `value` against `schema`. Throws ex-info with humanized errors on failure."
  [schema value]
  (when-let [err (some-> (m/explain schema value) me/humanize)]
    (throw (ex-info "Invalid input"
                    {:type   ::validation
                     :errors err}))))

(defn request
  "Issue an HTTP request to Twilio.

  `cfg`    - config map; only `:http-opts` is used here (overrides clj-http defaults)
  `method` - :get/:post/...
  `url`    - full request URL
  `req`    - clj-http request map (e.g. :basic-auth, :headers, :form-params)

  Returns parsed body on 2xx, throws ex-info with :status, :body, :url otherwise."
  [cfg method url req]
  (let [opts (merge default-http-opts
                    {:method method
                     :url    url}
                    req
                    (:http-opts cfg))
        {:keys [status body]} (http/request opts)]
    (if (<= 200 status 299)
      body
      (throw (ex-info (str "Twilio request failed: " status)
                      {:type   ::http-error
                       :status status
                       :body   body
                       :url    url})))))
