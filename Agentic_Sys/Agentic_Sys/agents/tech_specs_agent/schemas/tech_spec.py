from pydantic import BaseModel, Field, model_validator
from typing import List, Optional


class EntityField(BaseModel):
    name: str
    type: str
    description: str = ""


class Entity(BaseModel):
    name: str
    fields: List[EntityField]
    relationships: List[str] = Field(default_factory=list)


class DataModel(BaseModel):
    entities: List[Entity]

    @model_validator(mode="after")
    def must_have_entities(self):
        if not self.entities:
            raise ValueError("At least one entity is required in the data model")
        return self


class APIEndpoint(BaseModel):
    method: str = Field(..., pattern="^(GET|POST|PUT|PATCH|DELETE)$")
    path: str
    description: str
    request_body: str = ""
    response_body: str = ""


class APIDesign(BaseModel):
    endpoints: List[APIEndpoint]


class Component(BaseModel):
    name: str
    responsibility: str
    depends_on: List[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def must_have_responsibility(self):
        if not self.responsibility or not self.responsibility.strip():
            raise ValueError(f"Component '{self.name}' must have a responsibility")
        return self


class TechStack(BaseModel):
    frontend: str = ""
    backend: str = ""
    database: str = ""
    infrastructure: str = ""


class SystemOverview(BaseModel):
    name: str
    description: str
    architecture_pattern: str
    key_decisions: List[str] = Field(default_factory=list)


class Risk(BaseModel):
    risk: str
    impact: str
    mitigation: str


class TechSpecArtifact(BaseModel):
    system_overview: SystemOverview
    data_model: DataModel
    api_design: APIDesign
    components: List[Component]
    tech_stack: TechStack
    risks_and_mitigations: List[Risk] = Field(default_factory=list)
    open_questions: List[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def semantic_integrity_checks(self):
        if not self.components:
            raise ValueError("At least one component is required")
        if not self.system_overview.name or not self.system_overview.name.strip():
            raise ValueError("System overview must have a name")
        return self
