/**
 * マスタに関する型定義
 */

export type Department = {
    departmentId: number
    startDate: string
    departmentName: string
    endDate: string | null
};

export type Position = {
    positionId: number
    startDate: string
    positionName: string
    positionAllowance: number
    endDate: string | null
};

export type Qualification = {
    qualificationId: number
    startDate: string
    qualificationName: string
    isAdvance: boolean
    qualificationAllowance: number
    endDate: string | null
};

export type SkillGrade = {
    skillGrade: number
    startDate: string
    allowance: number
    endDate: string | null
};

export type Holiday = {
    date: string
    holidayType: string
    holidayName: string
};

export type CreateDepartmentRequest = {
    departmentName: string;
    startDate: string;
};

export type UpdateDepartmentRequest = {
    departmentId: number;
    departmentName: string;
    startDate: string;
};

export type CreatePositionRequest = {
    positionName: string;
    positionAllowance: number;
    startDate: string;
};

export type UpdatePositionRequest = {
    positionId: number;
    positionName: string;
    positionAllowance: number;
    startDate: string;
};

export type CreateQualificationRequest = {
    qualificationName: string;
    isAdvance: boolean;
    qualificationAllowance: number;
    startDate: string;
};

export type UpdateQualificationRequest = {
    qualificationId: number;
    qualificationName: string;
    isAdvance: boolean;
    qualificationAllowance: number;
    startDate: string;
};

export type UpdateSkillGradeRequest = {
    skillGrade: number;
    allowance: number;
    startDate: string;
};