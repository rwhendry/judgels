import * as React from 'react';

import { UserRef } from 'components/UserRef/UserRef';
import { ProfilesMap } from 'modules/api/jophiel/profile';

import './ContestContestantRemoveResultTable.css';

export interface ContestContestantRemoveResultTableProps {
  usernames: string[];
  deletedContestantProfilesMap: ProfilesMap;
}

export class ContestContestantRemoveResultTable extends React.PureComponent<ContestContestantRemoveResultTableProps> {
  render() {
    return (
      <>
        {this.renderContestantsTable('Removed contestants', this.props.deletedContestantProfilesMap)}
        {this.renderUnknownContestantsTable()}
      </>
    );
  }

  private renderContestantsTable = (title: string, profilesMap: ProfilesMap) => {
    const usernames = Object.keys(profilesMap)
      .slice()
      .sort((u1, u2) => u1.localeCompare(u2));

    if (usernames.length === 0) {
      return null;
    }

    const rows = usernames.map(username => (
      <tr key={username}>
        <td>
          <UserRef profile={profilesMap[username]} />
        </td>
      </tr>
    ));

    return (
      <>
        <h5>
          {title} ({usernames.length})
        </h5>
        <table className="bp3-html-table bp3-html-table-striped table-list-condensed contest-contestant-remove-result-table">
          <tbody>{rows}</tbody>
        </table>
      </>
    );
  };

  private renderUnknownContestantsTable = () => {
    const knownUsernames = Object.keys(this.props.deletedContestantProfilesMap);
    const usernames = this.props.usernames
      .filter(u => knownUsernames.indexOf(u) === -1)
      .slice()
      .sort((u1, u2) => u1.localeCompare(u2));

    if (usernames.length === 0) {
      return null;
    }

    const rows = usernames.map(username => (
      <tr key={username}>
        <td>{username}</td>
      </tr>
    ));

    return (
      <>
        <h5>Not contestants ({usernames.length})</h5>
        <table className="bp3-html-table bp3-html-table-striped table-list-condensed contest-contestant-remove-result-table">
          <tbody>{rows}</tbody>
        </table>
      </>
    );
  };
}
