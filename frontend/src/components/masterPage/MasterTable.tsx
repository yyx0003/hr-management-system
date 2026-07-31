type Column<T> = {
    header: string;
    render: (row: T) => React.ReactNode;
};

type Props<T> = {
    rows: T[];
    columns: Column<T>[];
    onSelect: (row: T) => void;
    selected?: T | null;
};

export function MasterTable<T>({
    rows,
    columns,
    onSelect,
}: Props<T>) {
    return (
        <table className="master-table">
            <thead>
                <tr>
                    {columns.map(
                        column => (
                        <th key={column.header}>
                                {column.header}
                        </th>
                        ))}
                </tr>
            </thead>

            <tbody>
                {rows.map(
                    (row, index) => (
                        <tr key={index}
                            onClick={() =>
                                onSelect(
                                    row
                                )}
                        >
                            {columns.map(
                                column => (
                                    <td key={column.header}>
                                        {column.render(row)}
                                    </td>),)}
                        </tr>
                    ))}
            </tbody>
        </table>
    );
}