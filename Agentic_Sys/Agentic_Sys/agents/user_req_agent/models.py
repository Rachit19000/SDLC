from __future__ import annotations
from pydantic import BaseModel, Field, model_validator
from typing import List, Set


class FunctionalRequirement(BaseModel):
    id: str = Field(..., pattern=r"^FR[0-9]+$")
    title: str
    description: str


class NonFunctionalRequirement(BaseModel):
    id: str = Field(..., pattern=r"^NFR[0-9]+$")
    description: str


class AcceptanceCriteria(BaseModel):
    id: str
    text: str
    references: List[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def criteria_must_be_non_empty(self):
        if not self.text or not self.text.strip():
            raise ValueError("Acceptance criteria text must be present and non-empty")
        return self


class RequirementsArtifact(BaseModel):
    system_name: str
    functional_requirements: List[FunctionalRequirement]
    nonfunctional_requirements: List[NonFunctionalRequirement]
    acceptance_criteria: List[AcceptanceCriteria]

    @model_validator(mode="after")
    def semantic_integrity_checks(self):
        if not self.system_name or not self.system_name.strip():
            raise ValueError("system_name must be provided")

        if not self.functional_requirements:
            raise ValueError("At least one functional requirement is required")

        if not self.acceptance_criteria:
            raise ValueError("At least one acceptance criteria is required")

        # Collect FR ids
        fr_ids: Set[str] = {fr.id for fr in self.functional_requirements}

        # Ensure FR ids are unique
        if len(fr_ids) != len(self.functional_requirements):
            raise ValueError("Duplicate Functional Requirement ids detected")

        # Validate acceptance criteria references (if provided)
        for ac in self.acceptance_criteria:
            for ref in ac.references:
                if ref not in fr_ids:
                    raise ValueError(f"Acceptance criteria {ac.id} references unknown FR id: {ref}")

        # Validate NFR list presence (must be present; can be empty list)
        if self.nonfunctional_requirements is None:
            raise ValueError("nonfunctional_requirements must be present (can be empty list if none)")

        return self
