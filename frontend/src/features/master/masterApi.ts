import { MASTER_MESSAGES } from "./messages";
import type { CreateDepartmentRequest, CreatePositionRequest, CreateQualificationRequest, Department, Position, Qualification, SkillGrade, UpdateDepartmentRequest, UpdatePositionRequest, UpdateQualificationRequest, UpdateSkillGradeRequest } from "./types";

export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '';

export const getHeaders = () => ({
    'Content-Type': 'application/json',
});

export const handleError = async (
    response: Response,
    defaultMessage: string,
) => {
    if (!response.ok) {
        const errorData =
            await response.json().catch(() => null);
        throw new Error(
            errorData?.message ?? defaultMessage
        )
    };
}

export const departmentApi = {

    /**
     * 全部署を取得
     * @returns 過去分を含めた全部署
     */
    findAll: async (): Promise<Department[]> => {
        const response = await fetch(`${API_BASE_URL}/department`, {
            method: 'GET',
        })
        await handleError(response, MASTER_MESSAGES.dataFetchFailed);
        return response.json();
    },

    /**
     * 引数に与えられた日付時点で有効な部署をすべて取得する
     * @param targetDate 検索する日付
     * @returns targetDate時点での有効な部署リスト
     */
    findAllEffectiveAt: async (targetDate: string): Promise<Department[]> => {
        const response = await fetch(`${API_BASE_URL}/department/${targetDate}`, {
            method: 'GET',
        })

        await handleError(response, MASTER_MESSAGES.dataFetchFailed);
        return response.json();
    },

    createDepartment: async (department: CreateDepartmentRequest): Promise<Department> => {
        const response = await fetch(`${API_BASE_URL}/department`, {
            headers: getHeaders(),
            method: 'POST',
            body: JSON.stringify(department),
        })
        await handleError(response, MASTER_MESSAGES.createFailed);
        return response.json();
    },

    updateDepartment: async (department: UpdateDepartmentRequest): Promise<Department> => {
        const response = await fetch(`${API_BASE_URL}/department`, {
            headers: getHeaders(),
            method: 'PUT',
            body: JSON.stringify(department),
        })
        await handleError(response, MASTER_MESSAGES.updateFailed);
        return response.json();
    },

    deleteDepartment: async (departmentId: number, startDate: string): Promise<void> => {
        const response = await fetch(
            `${API_BASE_URL}/department/${departmentId}/${startDate}`, {
            headers: getHeaders(),
            method: 'DELETE',
        })
        await handleError(response, MASTER_MESSAGES.deleteFailed);
    }
}

export const positionApi = {

    /**
     * 全役職履歴取得
     */
    findAll: async (): Promise<Position[]> => {

        const response = await fetch(
            `${API_BASE_URL}/position`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 指定日時点で有効な役職一覧取得
     */
    findAllEffectiveAt: async (
        targetDate: string,
    ): Promise<Position[]> => {

        const response = await fetch(
            `${API_BASE_URL}/position/${targetDate}`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 新規役職登録
     */
    createPosition: async (
        position: CreatePositionRequest,
    ): Promise<Position> => {

        const response = await fetch(
            `${API_BASE_URL}/position`,
            {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(position),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.createFailed,
        );

        return response.json();
    },

    /**
     * 役職履歴更新
     */
    updatePosition: async (
        position: UpdatePositionRequest,
    ): Promise<Position> => {

        const response = await fetch(
            `${API_BASE_URL}/position`,
            {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(position),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.updateFailed,
        );

        return response.json();
    },

    /**
     * 役職履歴削除
     */
    deletePosition: async (
        positionId: number,
        startDate: string,
    ): Promise<void> => {

        const response = await fetch(
            `${API_BASE_URL}/position/${positionId}/${startDate}`,
            {
                method: 'DELETE',
                headers: getHeaders(),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.deleteFailed,
        );
    },
};

export const qualificationApi = {

    /**
     * 全資格履歴取得
     */
    findAll: async (): Promise<Qualification[]> => {

        const response = await fetch(
            `${API_BASE_URL}/qualification`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 指定日時点で有効な資格一覧取得
     */
    findAllEffectiveAt: async (
        targetDate: string,
    ): Promise<Qualification[]> => {

        const response = await fetch(
            `${API_BASE_URL}/qualification/${targetDate}`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 新規資格登録
     */
    createQualification: async (
        qualification: CreateQualificationRequest,
    ): Promise<Qualification> => {

        const response = await fetch(
            `${API_BASE_URL}/qualification`,
            {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(
                    qualification,
                ),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.createFailed,
        );

        return response.json();
    },

    /**
     * 資格履歴更新
     */
    updateQualification: async (
        qualification: UpdateQualificationRequest,
    ): Promise<Qualification> => {

        const response = await fetch(
            `${API_BASE_URL}/qualification`,
            {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(
                    qualification,
                ),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.updateFailed,
        );

        return response.json();
    },

    /**
     * 資格履歴削除
     */
    deleteQualification: async (
        qualificationId: number,
        startDate: string,
    ): Promise<void> => {

        const response = await fetch(
            `${API_BASE_URL}/qualification/${qualificationId}/${startDate}`,
            {
                method: 'DELETE',
                headers: getHeaders(),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.deleteFailed,
        );
    },
};

export const skillGradeApi = {

    /**
     * 全職能資格履歴取得
     */
    findAll: async (): Promise<SkillGrade[]> => {

        const response = await fetch(
            `${API_BASE_URL}/skillgrade`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 指定日時点で有効な職能資格一覧取得
     */
    findAllEffectiveAt: async (
        targetDate: string,
    ): Promise<SkillGrade[]> => {

        const response = await fetch(
            `${API_BASE_URL}/skillgrade/${targetDate}`,
            {
                method: 'GET',
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.dataFetchFailed,
        );

        return response.json();
    },

    /**
     * 職能資格履歴更新
     */
    updateSkillGrade: async (
        skillGrade: UpdateSkillGradeRequest,
    ): Promise<SkillGrade> => {

        const response = await fetch(
            `${API_BASE_URL}/skillgrade`,
            {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify(
                    skillGrade,
                ),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.updateFailed,
        );

        return response.json();
    },

    /**
     * 職能資格履歴削除
     */
    deleteSkillGrade: async (
        skillGrade: number,
        startDate: string,
    ): Promise<void> => {

        const response = await fetch(
            `${API_BASE_URL}/skillgrade/${skillGrade}/${startDate}`,
            {
                method: 'DELETE',
                headers: getHeaders(),
            }
        );

        await handleError(
            response,
            MASTER_MESSAGES.deleteFailed,
        );
    },
};
