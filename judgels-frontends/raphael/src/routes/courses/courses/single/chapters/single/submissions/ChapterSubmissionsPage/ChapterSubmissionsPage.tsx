import { push } from 'connected-react-router';
import { parse, stringify } from 'query-string';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import { LoadingState } from '../../../../../../../../components/LoadingState/LoadingState';
import { ContentCard } from '../../../../../../../../components/ContentCard/ContentCard';
import Pagination from '../../../../../../../../components/Pagination/Pagination';
import SubmissionUserFilter from '../../../../../../../../components/SubmissionUserFilter/SubmissionUserFilter';
import { SubmissionFilterWidget } from '../../../../../../../../components/SubmissionFilterWidget/SubmissionFilterWidget';
import { AppState } from '../../../../../../../../modules/store';
import { Course } from '../../../../../../../../modules/api/jerahmeel/course';
import { CourseChapter } from '../../../../../../../../modules/api/jerahmeel/courseChapter';
import { SubmissionsResponse } from '../../../../../../../../modules/api/jerahmeel/submissionProgramming';
import { ChapterSubmissionsTable } from '../ChapterSubmissionsTable/ChapterSubmissionsTable';
import { selectUserJid } from '../../../../../../../../modules/session/sessionSelectors';
import { selectCourse } from '../../../../../modules/courseSelectors';
import { selectCourseChapter } from '../../../modules/courseChapterSelectors';
import { chapterSubmissionActions as injectedChapterSubmissionActions } from '../modules/chapterSubmissionActions';

export interface ChapterSubmissionsPageProps extends RouteComponentProps<{}> {
  userJid: string;
  course: Course;
  chapter: CourseChapter;
  onGetProgrammingSubmissions: (
    chapterJid: string,
    userJid?: string,
    problemJid?: string,
    page?: number
  ) => Promise<SubmissionsResponse>;
  onAppendRoute: (queries) => any;
}

interface ChapterSubmissionsFilter {
  problemAlias?: string;
}

interface ChapterSubmissionsPageState {
  response?: SubmissionsResponse;
  filter?: ChapterSubmissionsFilter;
  isFilterLoading?: boolean;
}

export class ChapterSubmissionsPage extends React.PureComponent<
  ChapterSubmissionsPageProps,
  ChapterSubmissionsPageState
> {
  private static PAGE_SIZE = 20;

  state: ChapterSubmissionsPageState = {};

  async componentDidMount() {
    const queries = parse(this.props.location.search);
    const problemAlias = queries.problemAlias as string;

    if (problemAlias) {
      await this.refreshSubmissions();
    }

    this.setState({ filter: { problemAlias } });
  }

  async componentDidUpdate(prevProps: ChapterSubmissionsPageProps) {
    if (prevProps.location.pathname !== this.props.location.pathname) {
      await this.refreshSubmissions();
      this.setState({ filter: {} });
    }
  }

  render() {
    return (
      <ContentCard>
        <h3>Submissions</h3>
        <hr />
        {this.renderUserFilter()}
        {this.renderFilterWidget()}
        <div className="clearfix" />
        {this.renderSubmissions()}
        {this.renderPagination()}
      </ContentCard>
    );
  }

  private renderUserFilter = () => {
    return <SubmissionUserFilter />;
  };

  private isUserFilterAll = () => {
    return (this.props.location.pathname + '/').includes('/all/');
  };

  private renderSubmissions = () => {
    const { response } = this.state;
    if (!response) {
      return <LoadingState />;
    }

    const { data: submissions, config, profilesMap, problemAliasesMap } = response;
    if (submissions.totalCount === 0) {
      return (
        <p>
          <small>No submissions.</small>
        </p>
      );
    }

    return (
      <ChapterSubmissionsTable
        course={this.props.course}
        chapter={this.props.chapter}
        submissions={submissions.page}
        canManage={config.canManage}
        profilesMap={profilesMap}
        problemAliasesMap={problemAliasesMap}
      />
    );
  };

  private renderPagination = () => {
    const { filter } = this.state;
    if (!filter) {
      return null;
    }

    // updates pagination when the filter is updated
    const key = '' + filter.problemAlias + this.isUserFilterAll();

    return (
      <Pagination
        key={key}
        currentPage={1}
        pageSize={ChapterSubmissionsPage.PAGE_SIZE}
        onChangePage={this.onChangePage}
      />
    );
  };

  private onChangePage = async (nextPage: number) => {
    const { problemAlias } = this.state.filter;
    const data = await this.refreshSubmissions(problemAlias, nextPage);
    return data.totalCount;
  };

  private refreshSubmissions = async (problemAlias?: string, page?: number) => {
    const userJid = this.isUserFilterAll() ? undefined : this.props.userJid;
    const { problemJid } = this.getFilterJids(problemAlias);
    const response = await this.props.onGetProgrammingSubmissions(
      this.props.chapter.chapterJid,
      userJid,
      problemJid,
      page
    );
    this.setState({ response, isFilterLoading: false });
    return response.data;
  };

  private getFilterJids = (problemAlias?: string) => {
    const { response } = this.state;
    if (!response) {
      return {};
    }

    const { config, problemAliasesMap } = response;
    const { problemJids } = config;

    const problemJid = problemJids.find(jid => problemAliasesMap[jid] === problemAlias);
    return { problemJid };
  };

  private renderFilterWidget = () => {
    const { response, filter, isFilterLoading } = this.state;
    if (!response || !filter) {
      return null;
    }
    const { config, problemAliasesMap } = response;
    const { problemJids } = config;

    const { problemAlias } = filter;
    return (
      <SubmissionFilterWidget
        problemAliases={problemJids.map(jid => problemAliasesMap[jid])}
        problemAlias={problemAlias}
        onFilter={this.onFilter}
        isLoading={!!isFilterLoading}
      />
    );
  };

  private onFilter = async filter => {
    const { problemAlias } = filter;
    this.setState(prevState => {
      const prevFilter = prevState.filter || {};
      return {
        filter,
        isFilterLoading: prevFilter.problemAlias !== problemAlias,
      };
    });
    this.props.onAppendRoute(filter);
  };
}

export function createChapterSubmissionsPage(chapterSubmissionActions) {
  const mapStateToProps = (state: AppState) => ({
    userJid: selectUserJid(state),
    course: selectCourse(state),
    chapter: selectCourseChapter(state),
  });

  const mapDispatchToProps = {
    onGetProgrammingSubmissions: chapterSubmissionActions.getSubmissions,
    onAppendRoute: queries => push({ search: stringify(queries) }),
  };

  return withRouter<any, any>(connect(mapStateToProps, mapDispatchToProps)(ChapterSubmissionsPage));
}

export default createChapterSubmissionsPage(injectedChapterSubmissionActions);
