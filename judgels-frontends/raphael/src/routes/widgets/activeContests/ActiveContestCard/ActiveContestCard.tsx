import * as React from 'react';
import { FormattedRelative } from 'react-intl';

import { TimeanddateLink } from '../../../../components/TimeanddateLink/TimeanddateLink';
import { ContentCardLink } from '../../../../components/ContentCardLink/ContentCardLink';
import { Contest } from '../../../../modules/api/uriel/contest';

import './ActiveContestCard.css';

export interface ActiveContestCardProps {
  contest: Contest;
}

export class ActiveContestCard extends React.PureComponent<ActiveContestCardProps> {
  render() {
    const { contest } = this.props;

    return (
      <ContentCardLink to={`/contests/${contest.slug}`}>
        <h4 className="active-contest-card-name">{contest.name}</h4>
        <p className="active-contest-card-date">
          <small>{this.renderBeginTime(contest)}</small>
        </p>
      </ContentCardLink>
    );
  }

  private renderBeginTime = (contest: Contest) => {
    let text = <>in progress</>;
    if (new Date().getTime() < contest.beginTime) {
      text = (
        <>
          starts <FormattedRelative value={contest.beginTime} />
        </>
      );
    }
    return (
      <TimeanddateLink time={contest.beginTime} message={contest.name}>
        {text}
      </TimeanddateLink>
    );
  };
}
