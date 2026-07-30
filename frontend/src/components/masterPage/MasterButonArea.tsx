type Props = {
    onCreate: () => void;
    onUpdate: () => void;
    onDelete: () => void;
    onClear: () => void;
    onBack: () => void;

    createDisabled?: boolean;
    updateDisabled?: boolean;
    deleteDisabled?: boolean;
};

export function MasterButtonArea({
    onCreate,
    onUpdate,
    onDelete,
    onClear,
    onBack,
    createDisabled = false,
    updateDisabled = false,
    deleteDisabled = false,
}: Props) {

    return (
        <div className="button-area">

            <button
                onClick={onCreate}
                disabled={createDisabled}
            >
                登録
            </button>

            <button
                onClick={onUpdate}
                disabled={updateDisabled}
            >
                更新
            </button>

            <button
                onClick={onDelete}
                disabled={deleteDisabled}
            >
                削除
            </button>

            <button onClick={onClear}>
                クリア
            </button>

            <button onClick={onBack}>
                戻る
            </button>

        </div>
    );
}