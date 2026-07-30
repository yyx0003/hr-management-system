import { useNavigate } from "react-router-dom";
import { ROUTES } from "../../constants/routes";

type Props = {
    value:
        | 'department'
        | 'position'
        | 'qualification'
        | 'skillgrade';
};

export function MasterSelector({
    value,
}: Props) {

    const navigate = useNavigate();

    const handleChange = (
        e: React.ChangeEvent<HTMLSelectElement>,
    ) => {

        switch (e.target.value) {

            case 'department':
                navigate(
                    ROUTES.department,
                );
                break;

            case 'position':
                navigate(
                    ROUTES.position,
                );
                break;

            case 'qualification':
                navigate(
                    ROUTES.qualification,
                );
                break;

            case 'skillgrade':
                navigate(
                    ROUTES.skillgrade,
                );
                break;
        }
    };

    return (
        <select
            value={value}
            onChange={handleChange}
        >
            <option value="department">
                部署
            </option>

            <option value="position">
                役職
            </option>

            <option value="qualification">
                資格
            </option>

            <option value="skillgrade">
                職能資格
            </option>

        </select>
    );
}