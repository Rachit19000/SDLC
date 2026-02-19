from __future__ import annotations
from pydantic import BaseModel, Field, model_validator
from typing import List, Set, Optional


class UserStory(BaseModel):
    id: str = Field(..., pattern=r"^US[0-9]+$")
    title: str
    description: str
    acceptance_criteria: List[str]
    sprint_week: int = Field(..., ge=1, le=4)
    linked_fr_ids: List[str]

    @model_validator(mode="after")
    def validate_user_story(self):
        if not self.title or not self.title.strip():
            raise ValueError("User story title must be non-empty")
        if not self.description or not self.description.strip():
            raise ValueError("User story description must be non-empty")
        if not self.acceptance_criteria:
            raise ValueError("User story must have at least one acceptance criteria")
        if not self.linked_fr_ids:
            raise ValueError("User story must link to at least one FR")
        return self


class SprintPlan(BaseModel):
    week: int = Field(..., ge=1, le=4)
    user_stories: List[UserStory] = Field(default_factory=list)
    theme: str = ""

    @model_validator(mode="after")
    def validate_sprint(self):
        # Allow empty sprints (not all weeks may have stories)
        if not self.theme:
            self.theme = f"Sprint {self.week}"
        return self


class UserStoriesArtifact(BaseModel):
    system_name: str
    sprints: List[SprintPlan]
    total_user_stories: int
    estimated_effort_days: int

    @model_validator(mode="after")
    def semantic_integrity_checks(self):
        if not self.system_name or not self.system_name.strip():
            raise ValueError("system_name must be provided")

        if not self.sprints:
            raise ValueError("At least one sprint is required")

        # Filter out empty sprints
        non_empty_sprints = [s for s in self.sprints if s.user_stories]
        if not non_empty_sprints:
            raise ValueError("At least one sprint must have user stories")

        # Collect all user stories from all sprints
        all_stories: List[UserStory] = []
        us_ids: Set[str] = set()
        for sprint in self.sprints:
            for us in sprint.user_stories:
                if us.id in us_ids:
                    raise ValueError(f"Duplicate user story id: {us.id}")
                us_ids.add(us.id)
                all_stories.append(us)

        # Auto-fix total_user_stories if it doesn't match
        self.total_user_stories = len(all_stories)

        # Verify estimated_effort_days is reasonable
        if self.estimated_effort_days <= 0:
            self.estimated_effort_days = max(1, len(all_stories) * 2)

        return self
