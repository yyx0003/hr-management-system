/**
 * 選択対象の履歴の削除可否（有効、または過去かの判定のみ）
 * @param startDate 開始日
 * @param endDate 終了日
 * @returns 削除可否
 */

type HistoryItem = {
    startDate: string;
};

export const canDeleteHistory = (
    startDate: string,
    endDate: string | null,
): boolean => {
    const today = new Date();

    const start = new Date(startDate);

    if (isActiveHistory(startDate, endDate) || endDate != null) {
        return false;
    }

    return start > today;
};

/**
 * 現在有効な履歴かを判定
 * @param startDate 
 * @param endDate 
 * @returns 有効かの判定
 */
export const isActiveHistory = (
    startDate: string,
    endDate: string | null,
): boolean => {

    const today = new Date();

    return (
        new Date(startDate) <= today &&
        (
            endDate === null ||
            new Date(endDate) >= today
        )
    );
};

/**
 * ID降順 → 開始日昇順でソート
 *
 * @param items 対象データ
 * @param idGetter ID取得関数
 * 
 */
export const sortMasterHistory = <T extends HistoryItem>(
    items: T[],
    idGetter: (item: T) => number,
): T[] => {

    return [...items].sort((a, b) => {

        const idDiff =
            idGetter(a) - idGetter(b);

        if (idDiff !== 0) {
            return idDiff;
        }

        return (
            new Date(a.startDate).getTime()
            -
            new Date(b.startDate).getTime()
        );
    });
};

export const canCreate = (
    name: string,
    allowance: string,
    startDate: string,
): boolean => {
    return name != '' &&
        allowance != '' &&
        startDate != '';
}

export const canUpdate = (
    name: string,
    allowance: string,
    startDate: string,
    endDate: string
): boolean => {
    return name != '' &&
        allowance != '' &&
        startDate != '' &&
        endDate == '';
}
