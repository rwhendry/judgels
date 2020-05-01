import { Classes, Button, Intent, Dialog } from '@blueprintjs/core';
import * as React from 'react';

import { LoadingState } from '../../../../components/LoadingState/LoadingState';
import { ProblemSetProblemsTable } from '../ProblemSetProblemsTable/ProblemSetProblemsTable';
import { ProblemSet } from '../../../../modules/api/jerahmeel/problemSet';
import {
  ProblemSetProblemsResponse,
  ProblemSetProblem,
  ProblemSetProblemData,
} from '../../../../modules/api/jerahmeel/problemSetProblem';
import ProblemSetProblemEditForm, {
  ProblemSetProblemEditFormData,
} from '../ProblemSetProblemEditForm/ProblemSetProblemEditForm';
import { Alias } from '../../../../components/forms/validations';

export interface ProblemSetProblemEditDialogProps {
  isOpen: boolean;
  problemSet?: ProblemSet;
  onCloseDialog: () => void;
  onGetProblems: (problemSetJid: string) => Promise<ProblemSetProblemsResponse>;
  onSetProblems: (problemSetJid: string, data: ProblemSetProblemData[]) => Promise<void>;
}

interface ProblemSetProblemEditDialogState {
  response?: ProblemSetProblemsResponse;
  isEditing: boolean;
}

export class ProblemSetProblemEditDialog extends React.Component<
  ProblemSetProblemEditDialogProps,
  ProblemSetProblemEditDialogState
> {
  state: ProblemSetProblemEditDialogState = {
    isEditing: false,
  };

  componentDidMount() {
    this.refreshProblems();
  }

  async componentDidUpdate(prevProps: ProblemSetProblemEditDialogProps) {
    if (prevProps.problemSet !== this.props.problemSet) {
      this.refreshProblems();
    }
  }

  render() {
    const { isOpen } = this.props;
    return (
      <div className="content-card__section">
        <Dialog
          isOpen={isOpen}
          onClose={this.closeDialog}
          title="Edit problemset problems"
          canOutsideClickClose={false}
        >
          {this.renderDialogContent()}
        </Dialog>
      </div>
    );
  }

  private closeDialog = () => {
    this.props.onCloseDialog();
    this.setState({ isEditing: false });
  };

  private renderDialogContent = () => {
    const { response, isEditing } = this.state;
    if (!response) {
      return this.renderDialogForm(<LoadingState />, null);
    }

    if (isEditing) {
      const props: any = {
        validator: this.validateProblems,
        renderFormComponents: this.renderDialogForm,
        onSubmit: this.updateProblems,
        initialValues: { problems: this.serializeProblems(response.data, response.problemsMap) },
      };
      return <ProblemSetProblemEditForm {...props} />;
    } else {
      const content = <ProblemSetProblemsTable response={response} />;
      const submitButton = <Button data-key="edit" text="Edit" intent={Intent.PRIMARY} onClick={this.toggleEditing} />;
      return this.renderDialogForm(content, submitButton);
    }
  };

  private renderDialogForm = (content: JSX.Element, submitButton: JSX.Element) => (
    <>
      <div className={Classes.DIALOG_BODY}>{content}</div>
      <div className={Classes.DIALOG_FOOTER}>
        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
          <Button text="Cancel" onClick={this.closeDialog} />
          {submitButton}
        </div>
      </div>
    </>
  );

  private refreshProblems = async () => {
    if (this.props.isOpen) {
      const response = await this.props.onGetProblems(this.props.problemSet.jid);
      this.setState({ response });
    }
  };

  private toggleEditing = () => {
    this.setState(prevState => ({
      isEditing: !prevState.isEditing,
    }));
  };

  private updateProblems = async (data: ProblemSetProblemEditFormData) => {
    const problems = this.deserializeProblems(data.problems);
    await this.props.onSetProblems(this.props.problemSet.jid, problems);
    await this.refreshProblems();
    this.toggleEditing();
  };

  private serializeProblems = (problems: ProblemSetProblem[], problemsMap): string => {
    return problems.map(c => `${c.alias},${problemsMap[c.problemJid].slug},${c.type}`).join('\n');
  };

  private deserializeProblems = (problems: string): ProblemSetProblemData[] => {
    return problems
      .split('\n')
      .map(s => s.trim())
      .filter(s => s.length > 0)
      .map(s => s.split(','))
      .map(s => s.map(t => t.trim()))
      .map(
        s =>
          ({
            alias: s[0],
            slug: s[1],
            type: s[2],
          } as ProblemSetProblemData)
      );
  };

  private validateProblems = (value: string) => {
    const problems = value
      .split('\n')
      .map(s => s.trim())
      .filter(s => s.length > 0)
      .map(s => s.split(','))
      .map(s => s.map(t => t.trim()));

    const aliases: string[] = [];
    const slugs: string[] = [];

    for (const c of problems) {
      if (c.length !== 3) {
        return 'Each line must contain 3 comma-separated elements';
      }
      const alias = c[0];
      const aliasValidation = Alias(alias);
      if (aliasValidation) {
        return 'Problem aliases: ' + aliasValidation;
      }

      const slug = c[1];

      aliases.push(alias);
      slugs.push(slug);
    }

    if (new Set(aliases).size !== aliases.length) {
      return 'Problem aliases must be unique';
    }
    if (new Set(slugs).size !== slugs.length) {
      return 'Problem slugs must be unique';
    }

    return undefined;
  };
}
