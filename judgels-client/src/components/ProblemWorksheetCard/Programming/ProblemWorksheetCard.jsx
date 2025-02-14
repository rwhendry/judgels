import { ProblemStatementCard } from './ProblemStatementCard/ProblemStatementCard';
import { ProblemSubmissionCard } from './ProblemSubmissionCard/ProblemSubmissionCard';

import './ProblemWorksheetCard.scss';

export function ProblemWorksheetCard({
  alias,
  worksheet: { statement, limits, submissionConfig, reasonNotAllowedToSubmit },
  submissionWarning,
  gradingLanguage,
  onSubmit,
}) {
  const renderStatement = () => {
    return <ProblemStatementCard alias={alias} statement={statement} limits={limits} />;
  };

  const renderSubmission = () => {
    if (!onSubmit) {
      return null;
    }
    return (
      <ProblemSubmissionCard
        config={submissionConfig}
        onSubmit={onSubmit}
        reasonNotAllowedToSubmit={reasonNotAllowedToSubmit}
        submissionWarning={submissionWarning}
        preferredGradingLanguage={gradingLanguage}
      />
    );
  };

  return (
    <div className="programming-problem-worksheet">
      {renderStatement()}
      {renderSubmission()}
    </div>
  );
}
