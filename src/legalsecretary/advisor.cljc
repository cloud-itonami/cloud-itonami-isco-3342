(ns legalsecretary.advisor
  "Legal Secretary Advisor — the advisor named in this repository's
  README, proposing a legal-secretarial operation (log a document
  record, schedule a court operation from an attorney-supplied
  deadline, flag a confidentiality concern, or coordinate a supply
  order) from an attorney's case load and filing calendar. Swappable
  mock/llm; the advisor ONLY proposes — `legalsecretary.governor`
  checks attorney/case provenance, the closed op allowlist and the
  supply-cost ceiling independently, and always escalates
  confidentiality-concern flags regardless of confidence. Modeled on
  cloud-itonami-isco-3313's accountingsupport.advisor.

  A proposal: {:op :log-document-record|:schedule-court-operation|
                    :flag-confidentiality-concern|:coordinate-supply-order
               :effect :propose :case-id str :document-id str
               :document-type kw :operation-date str
               :attorney-supplied? boolean :concern-detail str
               :cost number :stake kw :confidence n :rationale str}

  The advisor NEVER proposes an op outside this closed allowlist —
  it never finalizes disclosure of privileged/confidential case
  information, never provides legal advice, and never sets a filing
  deadline or legal strategy on its own authority. A
  :schedule-court-operation proposal only ever logs a deadline the
  attorney has already supplied (`:attorney-supplied? true`); it never
  invents one."
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- default-rationale [{:keys [op case-id concern-detail]}]
  (str "proposed " (name op) " for case " case-id
       (when (seq concern-detail) (str " -- noted concern: " concern-detail))))

(defn- infer [_store {:keys [op stake case-id document-id document-type
                              operation-date attorney-supplied?
                              concern-detail cost]
                       :as request}]
  {:op op
   :effect :propose
   :case-id case-id
   :document-id document-id
   :document-type document-type
   :operation-date operation-date
   :attorney-supplied? (boolean attorney-supplied?)
   :concern-detail concern-detail
   :cost cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (default-rationale request)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a legal-secretarial advisor. Given a request, propose an
   :op from the closed allowlist (:log-document-record,
   :schedule-court-operation, :flag-confidentiality-concern,
   :coordinate-supply-order), the :case-id, and any op-specific
   fields, plus an honest :confidence and a :stake. Never propose
   finalizing disclosure of privileged or confidential case
   information, never propose legal advice, and never propose a
   filing deadline or legal strategy that was not attorney-supplied
   -- the governor independently checks attorney/case provenance,
   the closed op allowlist and the supply-cost ceiling. Flagging a
   confidentiality concern always requires human sign-off regardless
   of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
