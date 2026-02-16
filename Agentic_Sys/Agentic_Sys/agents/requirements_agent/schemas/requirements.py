from pydantic import BaseModel, Field, model_validator
from typing import List


class UserStory(BaseModel):
    id: str = Field(..., pattern="^US[0-9]+$")
    role: str
    feature: str
    benefit: str


class AcceptanceCriteria(BaseModel):
    story_id: str
    criteria: List[str]

    @model_validator(mode="after")
    def criteria_must_be_non_empty(self):
        if not self.criteria:
            raise ValueError("Acceptance criteria cannot be empty")
        return self


class RequirementsArtifact(BaseModel):
    user_stories: List[UserStory]
    acceptance_criteria: List[AcceptanceCriteria]
    assumptions: List[str]

    @model_validator(mode="after")
    def semantic_integrity_checks(self):
        if not self.user_stories:
            raise ValueError("At least one user story is required")

        story_ids = {s.id for s in self.user_stories}
        ac_story_ids = {a.story_id for a in self.acceptance_criteria}

        if story_ids != ac_story_ids:
            raise ValueError(
                "Every user story must have corresponding acceptance criteria"
            )

        if not self.assumptions:
            raise ValueError("Assumptions must be explicitly listed")

        return self
